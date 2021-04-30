/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.build.GeneratedJvmClass
import org.jetbrains.kotlin.incremental.storage.FileToCanonicalPathConverter
import org.jetbrains.kotlin.incremental.storage.ProtoMapValue
import org.jetbrains.kotlin.name.FqName
import java.io.File

class KotlinClassSnapshot private constructor(
    val packageFqName: FqName,
    val protoMapValue: ProtoMapValue?,
    val constantMap: Map<String, Any>?,
    val inlineFunctionMap: Map<String, Long>?
) {

    companion object {

        /**
         * Creates a [KotlinClassSnapshot] of the given class, or null if it does not contain valid Kotlin metadata (e.g., if it was
         * compiled from a .java file instead of a .kt file).
         */
        fun create(classContents: ByteArray, tmpDir: File): KotlinClassSnapshot? {
            // Temporary - remove later
            val fakeTargetOutputDir = File(tmpDir, "FakeTargetOutputDir").also { it.mkdirs() }
            val fakeTargetDataRoot = File(tmpDir, "FakeTargetDataRoot").also { it.mkdirs() }
            val fakeSourceFile = File(tmpDir, "FakeClass.kt")
            val fakeClassFile = File(tmpDir, "FakeClass.class")

            val localFileKotlinClass = LocalFileKotlinClass.create(fakeClassFile, classContents) ?: return null

            val incrementalJvmCache = IncrementalJvmCache(fakeTargetDataRoot, fakeTargetOutputDir, FileToCanonicalPathConverter)
            val generatedJvmClass = GeneratedJvmClass(listOf(fakeSourceFile), fakeClassFile, localFileKotlinClass)
            val changesCollector = ChangesCollector()
            incrementalJvmCache.saveFileToCache(generatedJvmClass, changesCollector)

            val key = localFileKotlinClass.className.internalName
            return KotlinClassSnapshot(
                packageFqName = localFileKotlinClass.className.packageFqName,
                protoMapValue = incrementalJvmCache.getProtoMapValue(key),
                constantMap = incrementalJvmCache.getConstantMapValue(key),
                inlineFunctionMap = incrementalJvmCache.getInlineFunctionMapValue(key)
            )
        }
    }
}
