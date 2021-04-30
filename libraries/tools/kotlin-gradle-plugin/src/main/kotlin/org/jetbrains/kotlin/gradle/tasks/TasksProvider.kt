/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.UnknownTaskException
import org.gradle.api.artifacts.transform.TransformParameters
import org.gradle.api.artifacts.transform.TransformSpec
import org.gradle.api.attributes.Attribute
import org.gradle.api.tasks.TaskCollection
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.mapKotlinTaskProperties
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinCompilationData
import org.jetbrains.kotlin.gradle.plugin.runOnceAfterEvaluated
import org.jetbrains.kotlin.gradle.plugin.sources.applyLanguageSettingsToKotlinOptions
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrLink
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrLinkWithWorkers

/**
 * Registers the task with [name] and [type] and initialization script [body]
 */
@JvmName("registerTaskOld")
@Deprecated("please use Project.registerTask", ReplaceWith("project.registerTask(name, type, emptyList(), body)"))
internal fun <T : Task> registerTask(project: Project, name: String, type: Class<T>, body: (T) -> (Unit)): TaskProvider<T> =
    project.registerTask(name, type, emptyList(), body)

internal inline fun <reified T : Task> Project.registerTask(
    name: String,
    args: List<Any> = emptyList(),
    noinline body: ((T) -> (Unit))? = null
): TaskProvider<T> =
    this@registerTask.registerTask(name, T::class.java, args, body)

internal fun <T : Task> Project.registerTask(
    name: String,
    type: Class<T>,
    constructorArgs: List<Any> = emptyList(),
    body: ((T) -> (Unit))? = null
): TaskProvider<T> {
    val resultProvider = project.tasks.register(name, type, *constructorArgs.toTypedArray())
    if (body != null) {
        resultProvider.configure(body)
    }
    return resultProvider
}

internal fun TaskProvider<*>.dependsOn(other: TaskProvider<*>) = configure { it.dependsOn(other) }

internal inline fun <reified S : Task> TaskCollection<in S>.withType(): TaskCollection<S> = withType(S::class.java)

/**
 * Locates a task by [name] and [type], without triggering its creation or configuration.
 */
internal inline fun <reified T : Task> Project.locateTask(name: String): TaskProvider<T>? =
    try {
        tasks.withType(T::class.java).named(name)
    } catch (e: UnknownTaskException) {
        null
    }

/**
 * Locates a task by [name] and [type], without triggering its creation or configuration or registers new task
 * with [name], type [T] and initialization script [body]
 */
internal inline fun <reified T : Task> Project.locateOrRegisterTask(name: String, noinline body: (T) -> (Unit)): TaskProvider<T> {
    return project.locateTask(name) ?: project.registerTask(name, T::class.java, body = body)
}

internal open class KotlinTasksProvider {
    open fun registerKotlinJVMTask(
        project: Project,
        name: String,
        compilation: KotlinCompilationData<*>,
        configureAction: (KotlinCompile) -> (Unit)
    ): TaskProvider<out KotlinCompile> {
        val properties = PropertiesProvider(project)
        val taskClass = taskOrWorkersTask<KotlinCompile, KotlinCompileWithWorkers>(properties)

        registerTransformsOnce(project)

        val result = project.registerTask(name, taskClass) { kotlinCompile ->
            configureAction(kotlinCompile)

            val classpathSnapshotConfiguration = project.configurations.create("_classpath_snapshot_configuration_${kotlinCompile.name}")
            project.dependencies.add(
                classpathSnapshotConfiguration.name,
                project.files(project.provider { kotlinCompile.compileClasspath })
            )
            kotlinCompile.classpathSnapshotFiles.from(
                classpathSnapshotConfiguration.incoming.artifactView { viewConfig ->
                    viewConfig.attributes.attribute(ARTIFACT_TYPE_ATTRIBUTE, CLASSPATH_SNAPSHOT_ARTIFACT_TYPE)
                }.files
            )
            kotlinCompile.classpathSnapshotDir.set(project.file("${project.buildDir}/kotlin/${kotlinCompile.name}/classpath-snapshot"))
        }

        configure(result, project, properties, compilation)
        return result
    }

    fun registerKotlinJSTask(
        project: Project,
        name: String,
        compilation: KotlinCompilationData<*>,
        configureAction: (Kotlin2JsCompile) -> Unit
    ): TaskProvider<out Kotlin2JsCompile> {
        val properties = PropertiesProvider(project)
        val taskClass = taskOrWorkersTask<Kotlin2JsCompile, Kotlin2JsCompileWithWorkers>(properties)
        val result = project.registerTask(name, taskClass) {
            configureAction(it)
        }
        configure(result, project, properties, compilation)
        return result
    }

    fun registerKotlinJsIrTask(
        project: Project,
        name: String,
        compilation: KotlinCompilationData<*>,
        configureAction: (KotlinJsIrLink) -> Unit
    ): TaskProvider<out KotlinJsIrLink> {
        val properties = PropertiesProvider(project)
        val taskClass = taskOrWorkersTask<KotlinJsIrLink, KotlinJsIrLinkWithWorkers>(properties)
        val result = project.registerTask(name, taskClass) {
            configureAction(it)
        }
        configure(result, project, properties, compilation)
        return result
    }

    fun registerKotlinCommonTask(
        project: Project,
        name: String,
        compilation: KotlinCompilationData<*>,
        configureAction: (KotlinCompileCommon) -> (Unit)
    ): TaskProvider<out KotlinCompileCommon> {
        val properties = PropertiesProvider(project)
        val taskClass = taskOrWorkersTask<KotlinCompileCommon, KotlinCompileCommonWithWorkers>(properties)
        val result = project.registerTask(name, taskClass) {
            configureAction(it)
        }
        configure(result, project, properties, compilation)
        return result
    }

    open fun configure(
        kotlinTaskHolder: TaskProvider<out AbstractKotlinCompile<*>>,
        project: Project,
        propertiesProvider: PropertiesProvider,
        compilation: KotlinCompilationData<*>
    ) {
        project.runOnceAfterEvaluated("apply properties and language settings to ${kotlinTaskHolder.name}", kotlinTaskHolder) {
            propertiesProvider.mapKotlinTaskProperties(kotlinTaskHolder.get())

            applyLanguageSettingsToKotlinOptions(
                compilation.languageSettings,
                (kotlinTaskHolder.get() as org.jetbrains.kotlin.gradle.dsl.KotlinCompile<*>).kotlinOptions
            )
        }
    }

    private fun registerTransformsOnce(project: Project) {
        if (project.extensions.extraProperties.has(TRANSFORMS_REGISTERED)) {
            return
        }
        project.extensions.extraProperties[TRANSFORMS_REGISTERED] = true

        project.dependencies.registerTransform(JarToJarSnapshotTransform::class.java) { spec: TransformSpec<TransformParameters.None> ->
            // TODO The `from` artifact type needs to match the actual type used in compile classpath
            spec.from.attribute(ARTIFACT_TYPE_ATTRIBUTE, JAR_ARTIFACT_TYPE)
            spec.to.attribute(ARTIFACT_TYPE_ATTRIBUTE, CLASSPATH_SNAPSHOT_ARTIFACT_TYPE)
        }
    }

    private inline fun <reified Task, reified WorkersTask : Task> taskOrWorkersTask(properties: PropertiesProvider): Class<out Task> =
        if (properties.parallelTasksInProject != true) Task::class.java else WorkersTask::class.java
}

internal class AndroidTasksProvider : KotlinTasksProvider() {
    override fun configure(
        kotlinTaskHolder: TaskProvider<out AbstractKotlinCompile<*>>,
        project: Project,
        propertiesProvider: PropertiesProvider,
        compilation: KotlinCompilationData<*>
    ) {
        super.configure(kotlinTaskHolder, project, propertiesProvider, compilation)
        kotlinTaskHolder.configure {
            it.useModuleDetection = true
        }
    }
}

val ARTIFACT_TYPE_ATTRIBUTE: Attribute<String> = Attribute.of("artifactType", String::class.java)
const val TRANSFORMS_REGISTERED = "kgp.internal.property.transforms.registered"
const val JAR_ARTIFACT_TYPE = "jar"
const val CLASSPATH_SNAPSHOT_ARTIFACT_TYPE = "jar-abi"
