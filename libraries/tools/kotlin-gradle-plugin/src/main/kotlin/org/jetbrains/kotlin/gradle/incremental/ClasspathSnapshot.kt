/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.incremental

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.incremental.ClassDescriptorCreator
import org.jetbrains.kotlin.incremental.ClassDescriptorCreationResult.Failure
import org.jetbrains.kotlin.incremental.ClassDescriptorCreationResult.Success
import org.jetbrains.kotlin.incremental.JavaClassesSerializerExtension
import org.jetbrains.kotlin.incremental.SerializedJavaClass
import org.jetbrains.kotlin.incremental.storage.ProtoMapValue
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.serialization.DescriptorSerializer
import java.io.*
import java.util.zip.ZipInputStream

class ClasspathSnapshot(
    val classpathEntrySnapshots: List<ClasspathEntrySnapshot>
)

class ClasspathEntrySnapshot(

    /**
     * Maps classes to their snapshots, where classes are identified by their relative paths (type [String]) inside the classpath entry
     * (directory or jar).
     */
    val classSnapshots: Map<String, ClassSnapshot> // Not sorted
) : Serializable {

    companion object {

        // INCR_KOTLIN_COMPILE_BOOKMARK
        // Step 1b [Gradle side]: Create classpath snapshots
        fun create(jarFile: File, tmpDir: File): ClasspathEntrySnapshot {
            val classes = readClassesInJar(jarFile)

            val classSnapshots = classes.mapValues { (relativePathToClass, classContents) ->
                ClassSnapshot.create(jarFile, relativePathToClass, classContents, tmpDir)
            }.filterValues { classSnapshot -> !classSnapshot.isEmpty() }

            return ClasspathEntrySnapshot(classSnapshots)
        }

        /**
         * Returns a map from classes to their contents, where classes are identified by their relative paths (type [String]) inside the
         * given jar.
         */
        private fun readClassesInJar(jarFile: File): Map<String, ByteArray> {
            val classes: MutableMap<String, ByteArray> = mutableMapOf()
            ZipInputStream(jarFile.inputStream().buffered()).use { zipInputStream ->
                while (true) {
                    val entry = zipInputStream.nextEntry ?: break
                    if (entry.name.endsWith(".class")) {
                        classes[entry.name] = zipInputStream.readBytes()
                    }
                }
            }
            return classes.toMap()
        }

        fun readFromFile(classpathEntrySnapshotFile: File): ClasspathEntrySnapshot {
            return ObjectInputStream(FileInputStream(classpathEntrySnapshotFile).buffered()).use {
                @Suppress("UNCHECKED_CAST")
                it.readObject() as ClasspathEntrySnapshot
            }
        }
    }

    fun writeToFile(classpathEntrySnapshotFile: File) {
        ObjectOutputStream(FileOutputStream(classpathEntrySnapshotFile).buffered()).use {
            it.writeObject(this)
        }
    }
}

sealed class ClassSnapshot(

    /**
     * The jar file containing the class from which this snapshot was created.
     *
     * Note that this property is not actually part of the class snapshot. It's included here only to track the origin of a snapshot for
     * debugging purposes.
     */
    val jarFile: File,

    /**
     * The relative path of this class under the classpath entry (directory or jar).
     *
     * Note that this property is not actually part of the class snapshot. It's included here only to track the origin of a snapshot for
     * debugging purposes.
     */
    val relativePathToClass: String
) : Serializable {

    /** Whether this snapshot contains no information extracted from the contents of the class. */
    open fun isEmpty() = false

    companion object {

        fun create(jarFile: File, relativePathToClass: String, classContents: ByteArray, tmpDir: File): ClassSnapshot {
            return createKotlinClassSnapshot(jarFile, relativePathToClass, classContents, tmpDir)
                ?: createJavaClassSnapshot(jarFile, relativePathToClass, classContents)
        }

        /**
         * Creates a [KotlinClassSnapshot] of the given class, or null if it does not contain valid Kotlin metadata (e.g., if it was
         * compiled from a .java file instead of a .kt file).
         */
        private fun createKotlinClassSnapshot(
            jarFile: File,
            relativePathToClass: String,
            classContents: ByteArray,
            tmpDir: File
        ): KotlinClassSnapshot? {
            val kotlinClassSnapshot = org.jetbrains.kotlin.incremental.KotlinClassSnapshot.create(classContents, tmpDir)
            return if (kotlinClassSnapshot != null) {
                KotlinClassSnapshot(
                    jarFile = jarFile,
                    relativePathToClass = relativePathToClass,
                    packageFqName = kotlinClassSnapshot.packageFqName,
                    protoMapValue = kotlinClassSnapshot.protoMapValue,
                    constantMap = kotlinClassSnapshot.constantMap,
                    inlineFunctionMap = kotlinClassSnapshot.inlineFunctionMap
                )
            } else {
                null
            }
        }

        /**
         * Creates a [JavaClassSnapshot] of the given class, or a [FailedClassSnapshot] if it failed for some reason.
         */
        private fun createJavaClassSnapshot(
            jarFile: File,
            relativePathToClass: String,
            classContents: ByteArray
        ): ClassSnapshot {
            return when (val result = ClassDescriptorCreator.create(relativePathToClass, classContents)) {
                is Success -> JavaClassSnapshot(jarFile, relativePathToClass, result.classDescriptor.createSerializedJavaClass())
                is Failure -> FailedClassSnapshot(jarFile, relativePathToClass, result.reason)
            }
        }

        private fun ClassDescriptor.createSerializedJavaClass(): SerializedJavaClass {
            val extension = JavaClassesSerializerExtension()
            val classProto = try {
                DescriptorSerializer.create(this, extension, null).classProto(this).build()
            } catch (e: Exception) {
                throw IllegalStateException(
                    "Error during writing proto for descriptor: ${DescriptorRenderer.DEBUG_TEXT.render(this)}",
                    e
                )
            }
            val (stringTable, qualifiedNameTable) = extension.stringTable.buildProto()
            return SerializedJavaClass(classProto, stringTable, qualifiedNameTable)
        }
    }
}

class KotlinClassSnapshot(
    jarFile: File,
    relativePathToClass: String,

    val packageFqName: FqName,
    val protoMapValue: ProtoMapValue?,
    val constantMap: Map<String, Any>?,
    val inlineFunctionMap: Map<String, Long>?
) : ClassSnapshot(jarFile, relativePathToClass) {

    override fun isEmpty(): Boolean {
        return protoMapValue == null && constantMap == null && inlineFunctionMap == null
    }
}

class JavaClassSnapshot(
    jarFile: File,
    relativePathToClass: String,

    val serializedJavaClass: SerializedJavaClass
) : ClassSnapshot(jarFile, relativePathToClass)

class FailedClassSnapshot(
    jarFile: File,
    relativePathToClass: String,

    val reason: String
) : ClassSnapshot(jarFile, relativePathToClass)
