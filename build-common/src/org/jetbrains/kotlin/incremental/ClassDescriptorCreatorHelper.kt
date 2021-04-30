/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.SourceFile
import org.jetbrains.kotlin.load.java.JavaClassFinder
import org.jetbrains.kotlin.load.java.sources.JavaSourceElement
import org.jetbrains.kotlin.load.java.sources.JavaSourceElementFactory
import org.jetbrains.kotlin.load.java.structure.JavaAnnotation
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaElement
import org.jetbrains.kotlin.load.java.structure.JavaPackage
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.BinaryJavaClass
import org.jetbrains.kotlin.load.kotlin.KotlinClassFinder
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.ErrorReporter
import org.jetbrains.kotlin.serialization.deserialization.builtins.BuiltInSerializerProtocol
import org.jetbrains.kotlin.serialization.deserialization.builtins.BuiltInsResourceLoader
import java.io.InputStream

class BinaryJavaClassFinder(val binaryJavaClass: BinaryJavaClass) : JavaClassFinder {

    override fun findClass(request: JavaClassFinder.Request): JavaClass? {
        val fullClassName = request.classId.asSingleFqName().asString()
        return if (fullClassName == binaryJavaClass.fqName.asString()) {
            binaryJavaClass
        } else null
    }

    override fun findPackage(fqName: FqName): JavaPackage {
        return CustomJavaPackage(fqName)
    }

    override fun knownClassNamesInPackage(packageFqName: FqName): Set<String>? = null
}

class CustomJavaPackage(override val fqName: FqName) : JavaPackage {
    override val subPackages: Collection<JavaPackage>
        get() = TODO("Not yet implemented")
    override val annotations: Collection<JavaAnnotation>
        get() = TODO("Not yet implemented")
    override val isDeprecatedInJavaDoc: Boolean
        get() = TODO("Not yet implemented")

    override fun getClasses(nameFilter: (Name) -> Boolean) = TODO("Not yet implemented")
    override fun findAnnotation(fqName: FqName) = TODO("Not yet implemented")
}

class CustomKotlinClassFinder : KotlinClassFinder {
    private val builtInsResourceLoader = BuiltInsResourceLoader()

    override fun findKotlinClassOrContent(classId: ClassId): KotlinClassFinder.Result? = null

    override fun findKotlinClassOrContent(javaClass: JavaClass): KotlinClassFinder.Result? = null

    override fun findMetadata(classId: ClassId): InputStream? = null

    override fun hasMetadataPackage(fqName: FqName): Boolean = false

    override fun findBuiltInsData(packageFqName: FqName): InputStream? {
        if (!packageFqName.startsWith(StandardNames.BUILT_INS_PACKAGE_NAME)) return null

        return builtInsResourceLoader.loadResource(BuiltInSerializerProtocol.getBuiltInsFilePath(packageFqName))
    }
}

object FakeKotlinClassFinder : KotlinClassFinder {
    override fun findKotlinClassOrContent(classId: ClassId): KotlinClassFinder.Result? = null

    override fun findKotlinClassOrContent(javaClass: JavaClass): KotlinClassFinder.Result = TODO("Not yet implemented")

    override fun findMetadata(classId: ClassId): InputStream = TODO("Not yet implemented")

    override fun hasMetadataPackage(fqName: FqName): Boolean = TODO("Not yet implemented")

    override fun findBuiltInsData(packageFqName: FqName): InputStream = TODO("Not yet implemented")
}

class NoSourceJavaSourceElement(override val javaElement: JavaElement) : JavaSourceElement {
    override fun getContainingFile(): SourceFile = SourceFile.NO_SOURCE_FILE
}

object NoSourceJavaSourceElementFactory : JavaSourceElementFactory {
    override fun source(javaElement: JavaElement): JavaSourceElement = NoSourceJavaSourceElement(javaElement)
}

object CustomErrorReporter : ErrorReporter {
    override fun reportIncompleteHierarchy(descriptor: ClassDescriptor, unresolvedSuperClasses: MutableList<String>) {
        // Ignore
    }

    override fun reportCannotInferVisibility(descriptor: CallableMemberDescriptor) {
        throw IllegalStateException("Cannot infer visibility for $descriptor")
    }
}