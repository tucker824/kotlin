/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.Task
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.gradle.dsl.KaptExtensionApi
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinTopLevelExtensionConfig
import java.io.File

interface KotlinJvmCompileApi : Task {

    fun setSource(sources: Any)

    fun source(vararg sources: Any): SourceTask

    fun setClasspath(classpath: FileCollection)

    @get:Internal
    val parentOptionsProperty: Property<KotlinJvmOptions>

    @get:Internal
    val customPluginOptions: Property<CompilerPluginOptions>

    @get:Input
    val moduleName: Property<String>

    @get:Internal // takes part in the compiler arguments
    val friendPaths: ConfigurableFileCollection

    @get:LocalState
    val taskBuildDirectory: DirectoryProperty

    @OutputDirectory
    fun getDestinationDirectory(): DirectoryProperty

    fun applyFrom(ext: KotlinTopLevelExtensionConfig)
}

interface KaptGenerateStubsTaskApi : KotlinJvmCompileApi {
    @get:OutputDirectory
    val generatedStubsDir: DirectoryProperty

    fun applyFrom(ext: KaptExtensionApi)
}

/*
* javaSourceRoots
* classpath
* kaptClasspath
* compiledSources
* sourceCompatibility
*/
interface KaptKotlinTaskApi : Task {
    @get:Classpath
    @get:InputFiles
    val kaptClasspath: ConfigurableFileCollection

    @get:LocalState
    @get:Optional
    val incAptCache: DirectoryProperty

    @get:OutputDirectory
    val classesDir: DirectoryProperty

    @get:OutputDirectory
    val destinationDir: DirectoryProperty

    /** Used in the model builder only. */
    @get:OutputDirectory
    val kotlinSourcesDestinationDir: DirectoryProperty

    @get:Nested
    val annotationProcessorOptionProviders: MutableList<Any>

    @get:CompileClasspath
    val classpath: ConfigurableFileCollection

    /** Use [source] as input, as only .java files should be taken into account. */
    @get:Internal
    val javaSourceRoots: ConfigurableFileCollection

    @get:Internal
    val compiledSources: ListProperty<File>

    fun applyFrom(kaptExtension: KaptExtensionApi)
}