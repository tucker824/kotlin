/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.artifacts.transform.InputArtifact
import org.gradle.api.artifacts.transform.TransformAction
import org.gradle.api.artifacts.transform.TransformOutputs
import org.gradle.api.artifacts.transform.TransformParameters
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Classpath
import org.jetbrains.kotlin.gradle.incremental.ClasspathEntrySnapshot
import java.io.File

abstract class JarToJarSnapshotTransform : TransformAction<TransformParameters.None> {

    @get:Classpath
    @get:InputArtifact
    abstract val inputArtifact: Provider<FileSystemLocation>

    override fun transform(outputs: TransformOutputs) {
        val inputJarFile = inputArtifact.get().asFile
        val outputJarSnapshotFile = outputs.file(JAR_SNAPSHOT_FILE_NAME)
        val tmpDir = File(outputJarSnapshotFile, "../tmp").canonicalFile // Temporary - remove later

        // INCR_KOTLIN_COMPILE_BOOKMARK
        // Step 1a [Gradle side]: Create classpath snapshots, using artifact transforms
        println("Creating jar snapshot for $inputJarFile")
        val jarSnapshot = ClasspathEntrySnapshot.create(inputJarFile, tmpDir)
        jarSnapshot.writeToFile(outputJarSnapshotFile)
    }
}

const val JAR_SNAPSHOT_FILE_NAME = "jar-snapshot.bin"
