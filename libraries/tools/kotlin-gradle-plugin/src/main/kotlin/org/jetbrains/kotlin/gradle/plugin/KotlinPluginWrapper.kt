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

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.GradleException
import org.gradle.api.NamedDomainObjectFactory
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.dsl.KaptExtensionApi
import org.jetbrains.kotlin.gradle.internal.KaptGenerateStubsTask
import org.jetbrains.kotlin.gradle.internal.KaptWithoutKotlincTask
import org.jetbrains.kotlin.gradle.logging.kotlinDebug
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMetadataTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMultiplatformPlugin
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinPm20GradlePlugin
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinPm20ProjectExtension
import org.jetbrains.kotlin.gradle.plugin.sources.DefaultKotlinSourceSetFactory
import org.jetbrains.kotlin.gradle.plugin.statistics.KotlinBuildStatsService
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsCompilerAttribute
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsPlugin
import org.jetbrains.kotlin.gradle.targets.js.npm.addNpmDependencyExtension
import org.jetbrains.kotlin.gradle.tasks.*
import org.jetbrains.kotlin.gradle.tasks.KOTLIN_COMPILER_EMBEDDABLE
import org.jetbrains.kotlin.gradle.tasks.KOTLIN_KLIB_COMMONIZER_EMBEDDABLE
import org.jetbrains.kotlin.gradle.tasks.KOTLIN_MODULE_GROUP
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.testing.internal.KotlinTestsRegistry
import org.jetbrains.kotlin.gradle.tooling.buildKotlinToolingMetadataTask
import org.jetbrains.kotlin.gradle.utils.checkGradleCompatibility
import org.jetbrains.kotlin.gradle.utils.loadPropertyFromResources
import org.jetbrains.kotlin.statistics.metrics.StringMetrics
import javax.inject.Inject
import kotlin.reflect.KClass

abstract class KotlinBasePlugin : Plugin<Project> {

    protected val log: Logger = Logging.getLogger(this.javaClass)
    val kotlinPluginVersion = getKotlinPluginVersion(log)

    open val projectExtensionClass: KClass<out KotlinTopLevelExtension> get() = KotlinProjectExtension::class

    internal open fun kotlinSourceSetFactory(project: Project): NamedDomainObjectFactory<KotlinSourceSet> =
        DefaultKotlinSourceSetFactory(project)

    override fun apply(project: Project) {
        val listenerRegistryHolder = BuildEventsListenerRegistryHolder.getInstance(project)
        val statisticsReporter = KotlinBuildStatsService.getOrCreateInstance(project)
        statisticsReporter?.report(StringMetrics.KOTLIN_COMPILER_VERSION, kotlinPluginVersion)

        checkGradleCompatibility()

        addKotlinCompilerConfiguration(project)
        project.configurations.maybeCreate(PLUGIN_CLASSPATH_CONFIGURATION_NAME)

        val kotlinGradleBuildServices = KotlinGradleBuildServices.getInstance(project, listenerRegistryHolder)
        kotlinGradleBuildServices.detectKotlinPluginLoadedInMultipleProjects(project, kotlinPluginVersion)

        setupAttributeMatchingStrategy(project)
    }

    private fun addKotlinCompilerConfiguration(project: Project) {
        project
            .configurations
            .maybeCreate(COMPILER_CLASSPATH_CONFIGURATION_NAME)
            .defaultDependencies {
                it.add(
                    project.dependencies.create("$KOTLIN_MODULE_GROUP:$KOTLIN_COMPILER_EMBEDDABLE:$kotlinPluginVersion")
                )
            }
        project
            .tasks
            .withType(AbstractKotlinCompileTool::class.java)
            .configureEach { task ->
                task.defaultCompilerClasspath.setFrom(
                    project.configurations.named(COMPILER_CLASSPATH_CONFIGURATION_NAME)
                )
            }
    }

    private fun setupAttributeMatchingStrategy(project: Project) = with(project.dependencies.attributesSchema) {
        KotlinPlatformType.setupAttributesMatchingStrategy(this)
        KotlinUsages.setupAttributesMatchingStrategy(project, this)
        KotlinJsCompilerAttribute.setupAttributesMatchingStrategy(project.dependencies.attributesSchema)
        ProjectLocalConfigurations.setupAttributesMatchingStrategy(this)
    }
}

abstract class KotlinBaseApiPlugin : KotlinBasePlugin(), KotlinJvmFactory {

    override val pluginVersion = kotlinPluginVersion

    override fun addCompilerPluginDependency(project: Project, dependency: Any) {
        project.dependencies.add(PLUGIN_CLASSPATH_CONFIGURATION_NAME, dependency)
    }

    override fun createKotlinJvmDsl(factory: (Class<out KotlinJvmOptions>) -> KotlinJvmOptions): KotlinJvmOptions {
        return KotlinJvmOptionsImpl()
    }

    override fun createKaptExtension(factory: (Class<out KaptExtensionApi>) -> KaptExtensionApi): KaptExtensionApi {
        return factory.invoke(KaptExtension::class.java)
    }

    override fun createKotlinProjectExtension(factory: (Class<out KotlinTopLevelExtensionConfig>) -> KotlinTopLevelExtensionConfig): KotlinTopLevelExtensionConfig {
        return factory.invoke(KotlinProjectExtension::class.java)
    }

    override fun createKotlinCompileTask(name: String, taskContainer: TaskContainer): TaskProvider<out KotlinJvmCompileApi> {
        return taskContainer.register(name, KotlinCompile::class.java)
    }

    override fun createKotlinGenerateStubsTask(name: String, taskContainer: TaskContainer): TaskProvider<out KaptGenerateStubsTaskApi> {
        return taskContainer.register(name, KaptGenerateStubsTask::class.java)
    }

    override fun createKotlinKaptTask(name: String, taskContainer: TaskContainer): TaskProvider<out KaptKotlinTaskApi> {
        return taskContainer.register(name, KaptWithoutKotlincTask::class.java)
    }

    private fun addKotlinCompilerConfiguration(project: Project) {
        project
            .configurations
            .maybeCreate(COMPILER_CLASSPATH_CONFIGURATION_NAME)
            .defaultDependencies {
                it.add(
                    project.dependencies.create("$KOTLIN_MODULE_GROUP:$KOTLIN_COMPILER_EMBEDDABLE:$kotlinPluginVersion")
                )
            }
        project
            .tasks
            .withType(AbstractKotlinCompileTool::class.java)
            .configureEach { task ->
                task.defaultCompilerClasspath.setFrom(
                    project.configurations.named(COMPILER_CLASSPATH_CONFIGURATION_NAME)
                )
            }
    }

    private fun setupAttributeMatchingStrategy(project: Project) = with(project.dependencies.attributesSchema) {
        KotlinPlatformType.setupAttributesMatchingStrategy(this)
        KotlinUsages.setupAttributesMatchingStrategy(project, this)
        KotlinJsCompilerAttribute.setupAttributesMatchingStrategy(project.dependencies.attributesSchema)
        ProjectLocalConfigurations.setupAttributesMatchingStrategy(this)
    }
}

abstract class KotlinBasePluginWrapper : KotlinBasePlugin() {

    override fun apply(project: Project) {
        super.apply(project)

        project.gradle.projectsEvaluated {
            whenBuildEvaluated(project)
        }

        project.configurations.maybeCreate(NATIVE_COMPILER_PLUGIN_CLASSPATH_CONFIGURATION_NAME).apply {
            isTransitive = false
        }
        project.configurations.maybeCreate(KLIB_COMMONIZER_CLASSPATH_CONFIGURATION_NAME).defaultDependencies {
            it.add(project.dependencies.create("$KOTLIN_MODULE_GROUP:$KOTLIN_KLIB_COMMONIZER_EMBEDDABLE:$kotlinPluginVersion"))
        }

        project.createKotlinExtension(projectExtensionClass).apply {
            coreLibrariesVersion = kotlinPluginVersion

            fun kotlinSourceSetContainer(factory: NamedDomainObjectFactory<KotlinSourceSet>) =
                project.container(KotlinSourceSet::class.java, factory)

            val topLevelExtension = project.topLevelExtension
            if (topLevelExtension is KotlinProjectExtension) {
                project.kotlinExtension.sourceSets = kotlinSourceSetContainer(kotlinSourceSetFactory(project))
            }
        }

        project.extensions.add(KotlinTestsRegistry.PROJECT_EXTENSION_NAME, createTestRegistry(project))

        val listenerRegistryHolder = BuildEventsListenerRegistryHolder.getInstance(project)
        val kotlinGradleBuildServices = KotlinGradleBuildServices.getInstance(project, listenerRegistryHolder)
        val plugin = getPlugin(project, kotlinGradleBuildServices)

        plugin.apply(project)

        project.addNpmDependencyExtension()

        project.buildKotlinToolingMetadataTask
    }

    private fun addKotlinCompilerConfiguration(project: Project) {
        project
            .configurations
            .maybeCreate(COMPILER_CLASSPATH_CONFIGURATION_NAME)
            .defaultDependencies {
                it.add(
                    project.dependencies.create("$KOTLIN_MODULE_GROUP:$KOTLIN_COMPILER_EMBEDDABLE:${project.getKotlinPluginVersion()}")
                )
            }
        project
            .tasks
            .withType(AbstractKotlinCompile::class.java)
            .configureEach { task ->
                task.defaultCompilerClasspath.setFrom(
                    project.configurations.named(COMPILER_CLASSPATH_CONFIGURATION_NAME)
                )
            }
    }

    open fun whenBuildEvaluated(project: Project) {
    }

    internal open fun createTestRegistry(project: Project) = KotlinTestsRegistry(project)

    private fun setupAttributeMatchingStrategy(project: Project) = with(project.dependencies.attributesSchema) {
        KotlinPlatformType.setupAttributesMatchingStrategy(this)
        KotlinUsages.setupAttributesMatchingStrategy(project, this)
        KotlinJsCompilerAttribute.setupAttributesMatchingStrategy(project.dependencies.attributesSchema)
        ProjectLocalConfigurations.setupAttributesMatchingStrategy(this)
    }

    internal abstract fun getPlugin(
        project: Project,
        kotlinGradleBuildServices: KotlinGradleBuildServices
    ): Plugin<Project>
}

open class KotlinPluginWrapper @Inject constructor(
    protected val registry: ToolingModelBuilderRegistry
) : KotlinBasePluginWrapper() {
    override fun getPlugin(project: Project, kotlinGradleBuildServices: KotlinGradleBuildServices): Plugin<Project> =
        KotlinPlugin(registry)

    override val projectExtensionClass: KClass<out KotlinJvmProjectExtension>
        get() = KotlinJvmProjectExtension::class
}

open class KotlinCommonPluginWrapper @Inject constructor(
    protected val registry: ToolingModelBuilderRegistry
) : KotlinBasePluginWrapper() {
    override fun getPlugin(project: Project, kotlinGradleBuildServices: KotlinGradleBuildServices): Plugin<Project> =
        KotlinCommonPlugin(registry)

    override val projectExtensionClass: KClass<out KotlinCommonProjectExtension>
        get() = KotlinCommonProjectExtension::class
}

open class KotlinAndroidPluginWrapper @Inject constructor(
    protected val registry: ToolingModelBuilderRegistry
) : KotlinBasePluginWrapper() {
    override fun getPlugin(project: Project, kotlinGradleBuildServices: KotlinGradleBuildServices): Plugin<Project> =
        KotlinAndroidPlugin(registry)

    override val projectExtensionClass: KClass<out KotlinAndroidProjectExtension>
        get() = KotlinAndroidProjectExtension::class
}

open class Kotlin2JsPluginWrapper @Inject constructor(
    protected val registry: ToolingModelBuilderRegistry
) : KotlinBasePluginWrapper() {
    override fun getPlugin(project: Project, kotlinGradleBuildServices: KotlinGradleBuildServices): Plugin<Project> =
        Kotlin2JsPlugin(registry)

    override val projectExtensionClass: KClass<out Kotlin2JsProjectExtension>
        get() = Kotlin2JsProjectExtension::class
}

open class KotlinJsPluginWrapper @Inject constructor(
) : KotlinBasePluginWrapper() {
    override fun getPlugin(project: Project, kotlinGradleBuildServices: KotlinGradleBuildServices): Plugin<Project> =
        KotlinJsPlugin(project.getKotlinPluginVersion())

    override val projectExtensionClass: KClass<out KotlinJsProjectExtension>
        get() = KotlinJsProjectExtension::class

    override fun whenBuildEvaluated(project: Project) {
        val isJsTargetUninitialized = (project.kotlinExtension as KotlinJsProjectExtension)
            ._target == null

        if (isJsTargetUninitialized) {
            throw GradleException(
                """
                Please initialize the Kotlin/JS target in '${project.name} (${project.path})'. Use:
                kotlin {
                    js {
                        // To build distributions and run tests for browser or Node.js use one or both of:
                        browser()
                        nodejs()
                    }
                }
                Read more https://kotlinlang.org/docs/reference/js-project-setup.html
                """.trimIndent()
            )
        }
    }

    override fun createTestRegistry(project: Project) = KotlinTestsRegistry(project, "test")
}

open class KotlinMultiplatformPluginWrapper @Inject constructor(
) : KotlinBasePluginWrapper() {
    override fun getPlugin(project: Project, kotlinGradleBuildServices: KotlinGradleBuildServices): Plugin<Project> =
        KotlinMultiplatformPlugin()

    override val projectExtensionClass: KClass<out KotlinMultiplatformExtension>
        get() = KotlinMultiplatformExtension::class

    override fun whenBuildEvaluated(project: Project) {
        val isNoTargetsInitialized = (project.kotlinExtension as KotlinMultiplatformExtension)
            .targets
            .none { it !is KotlinMetadataTarget }

        if (isNoTargetsInitialized) {
            throw GradleException(
                """
                Please initialize at least one Kotlin target in '${project.name} (${project.path})'.
                Read more https://kotlinlang.org/docs/reference/building-mpp-with-gradle.html#setting-up-targets
                """.trimIndent()
            )
        }
    }
}

open class KotlinPm20PluginWrapper @Inject constructor(private val objectFactory: ObjectFactory) : KotlinBasePluginWrapper() {
    override fun getPlugin(project: Project, kotlinGradleBuildServices: KotlinGradleBuildServices): Plugin<Project> =
        objectFactory.newInstance(KotlinPm20GradlePlugin::class.java)

    override val projectExtensionClass: KClass<out KotlinPm20ProjectExtension>
        get() = KotlinPm20ProjectExtension::class
}

fun Project.getKotlinPluginVersion(): String {
    return getKotlinPluginVersion(logger)
}

fun getKotlinPluginVersion(logger: Logger): String {
    if (!kotlinPluginVersionFromResources.isInitialized()) {
        logger.kotlinDebug("Loading version information")
        logger.kotlinDebug("Found project version [${kotlinPluginVersionFromResources.value}")
    }
    return kotlinPluginVersionFromResources.value
}

private fun loadKotlinPluginVersionFromResourcesOf(any: Any) =
    any.loadPropertyFromResources("project.properties", "project.version")

private val kotlinPluginVersionFromResources = lazy {
    loadKotlinPluginVersionFromResourcesOf(object {})
}