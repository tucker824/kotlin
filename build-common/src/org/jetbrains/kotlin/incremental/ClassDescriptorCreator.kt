/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import com.intellij.testFramework.LightVirtualFile
import org.jetbrains.kotlin.builtins.ReflectionTypes
import org.jetbrains.kotlin.builtins.jvm.JvmBuiltIns
import org.jetbrains.kotlin.builtins.jvm.JvmBuiltInsPackageFragmentProvider
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.CompositePackageFragmentProvider
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.load.java.AnnotationTypeQualifierResolver
import org.jetbrains.kotlin.load.java.JavaClassFinder
import org.jetbrains.kotlin.load.java.JavaClassesTracker
import org.jetbrains.kotlin.load.java.components.JavaPropertyInitializerEvaluator
import org.jetbrains.kotlin.load.java.components.JavaResolverCache
import org.jetbrains.kotlin.load.java.components.SignaturePropagator
import org.jetbrains.kotlin.load.java.lazy.*
import org.jetbrains.kotlin.load.java.structure.JavaAnnotation
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.BinaryClassSignatureParser
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.BinaryJavaClass
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.ClassifierResolutionContext
import org.jetbrains.kotlin.load.java.typeEnhancement.JavaTypeEnhancement
import org.jetbrains.kotlin.load.java.typeEnhancement.SignatureEnhancement
import org.jetbrains.kotlin.load.kotlin.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.jvm.JavaDescriptorResolver
import org.jetbrains.kotlin.resolve.sam.SamConversionResolverImpl
import org.jetbrains.kotlin.serialization.deserialization.ContractDeserializer
import org.jetbrains.kotlin.serialization.deserialization.DeserializationConfiguration
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.types.checker.NewKotlinTypeChecker
import org.jetbrains.kotlin.utils.JavaTypeEnhancementState

/** Creates [ClassDescriptor]s from .class files. */
object ClassDescriptorCreator {

    fun create(relativePathToClass: String, classContents: ByteArray): ClassDescriptorCreationResult {
        val binaryJavaClass = createBinaryJavaClass(relativePathToClass, classContents)
        return createClassDescriptor(binaryJavaClass)
    }
}

sealed class ClassDescriptorCreationResult {
    class Success(val classDescriptor: ClassDescriptor) : ClassDescriptorCreationResult()
    class Failure(val reason: String) : ClassDescriptorCreationResult()
}

private fun createBinaryJavaClass(relativePathToClass: String, classContents: ByteArray): BinaryJavaClass {
    val fullClassName = relativePathToClass.substringBeforeLast(".class").replace('/', '.').replace('\\', '.')
    val packageName = fullClassName.substringBeforeLast(".")
    val simpleClassName = fullClassName.substringAfterLast(".")
    return BinaryJavaClass(
        LightVirtualFile(),
        FqName(packageName).child(Name.identifier(simpleClassName)),
        ClassifierResolutionContext { null },
        BinaryClassSignatureParser(),
        outerClass = null,
        classContent = classContents
    )
}

private fun createClassDescriptor(binaryJavaClass: BinaryJavaClass): ClassDescriptorCreationResult {
    // This method body is a modified version of RuntimeModuleData.create()
    val classId = ClassId(binaryJavaClass.fqName.parent(), binaryJavaClass.fqName.shortName())
    if (classId.isLocal) {
        return ClassDescriptorCreationResult.Failure("ClassId.isLocal = true. ClassId = $classId")
    }

    val javaClassFinder = BinaryJavaClassFinder(binaryJavaClass)

    val storageManager = LockBasedStorageManager("RuntimeModuleData")
    val builtIns = JvmBuiltIns(storageManager, JvmBuiltIns.Kind.FROM_DEPENDENCIES)
    val moduleName = "<runtime module>"

    val moduleDescriptor = ModuleDescriptorImpl(Name.special(moduleName), storageManager, builtIns)
    builtIns.builtInsModule = moduleDescriptor

    builtIns.initialize(moduleDescriptor, isAdditionalBuiltInsFeatureSupported = true)

    val reflectKotlinClassFinder = FakeKotlinClassFinder
    val deserializedDescriptorResolver = DeserializedDescriptorResolver()
    val singleModuleClassResolver = SingleModuleClassResolver()
    val notFoundClasses = NotFoundClasses(storageManager, moduleDescriptor)

    val javaResolverComponents =
        createJavaResolverComponents(
            javaClassFinder,
            moduleDescriptor, storageManager, notFoundClasses,
            reflectKotlinClassFinder, deserializedDescriptorResolver, singleModuleClassResolver
        )
    val lazyJavaPackageFragmentProvider = LazyJavaPackageFragmentProvider(javaResolverComponents)

    val deserializationComponentsForJava =
        createDeserializationComponentsForJava(
            moduleDescriptor, storageManager, notFoundClasses, lazyJavaPackageFragmentProvider,
            reflectKotlinClassFinder, deserializedDescriptorResolver
        )
    deserializedDescriptorResolver.setComponents(deserializationComponentsForJava)

    val javaDescriptorResolver = JavaDescriptorResolver(lazyJavaPackageFragmentProvider, JavaResolverCache.EMPTY)
    singleModuleClassResolver.resolver = javaDescriptorResolver

    // .kotlin_builtins files should be found by the same class loader that loaded stdlib classes
//    val stdlibClassLoader = Unit::class.java.classLoader
    val builtinsProvider = JvmBuiltInsPackageFragmentProvider(
        storageManager,
        CustomKotlinClassFinder(),
        moduleDescriptor,
        notFoundClasses,
        builtIns.customizer,
        builtIns.customizer,
        DeserializationConfiguration.Default,
        NewKotlinTypeChecker.Default,
        SamConversionResolverImpl(storageManager, emptyList())
    )

    moduleDescriptor.setDependencies(moduleDescriptor)
    moduleDescriptor.initialize(CompositePackageFragmentProvider(listOf(javaDescriptorResolver.packageFragmentProvider, builtinsProvider)))

    val classDescriptor = moduleDescriptor.findClassAcrossModuleDependencies(classId)
        ?: return ClassDescriptorCreationResult.Failure("moduleDescriptor.findClassAcrossModuleDependencies returns null")
    return ClassDescriptorCreationResult.Success(classDescriptor)
}

private fun createJavaResolverComponents(
    javaClassFinder: JavaClassFinder,
    module: ModuleDescriptor,
    storageManager: StorageManager,
    notFoundClasses: NotFoundClasses,
    kotlinClassFinder: KotlinClassFinder,
    deserializedDescriptorResolver: DeserializedDescriptorResolver,
    singleModuleClassResolver: ModuleClassResolver,
    packagePartProvider: PackagePartProvider = PackagePartProvider.Empty
): JavaResolverComponents {
    val annotationTypeQualifierResolver = AnnotationTypeQualifierResolver(storageManager, JavaTypeEnhancementState.DISABLED_JSR_305)
    return JavaResolverComponents(
        storageManager,
        javaClassFinder,
        kotlinClassFinder,
        deserializedDescriptorResolver,
        SignaturePropagator.DO_NOTHING,
        CustomErrorReporter,
        JavaResolverCache.EMPTY,
        JavaPropertyInitializerEvaluator.DoNothing,
        SamConversionResolverImpl(storageManager, emptyList()),
        NoSourceJavaSourceElementFactory,
        singleModuleClassResolver,
        packagePartProvider,
        SupertypeLoopChecker.EMPTY,
        LookupTracker.DO_NOTHING,
        module,
        ReflectionTypes(module, notFoundClasses),
        annotationTypeQualifierResolver,
        SignatureEnhancement(
            annotationTypeQualifierResolver,
            JavaTypeEnhancementState.DISABLED_JSR_305,
            JavaTypeEnhancement(JavaResolverSettings.Default)
        ),
        JavaClassesTracker.Default,
        JavaResolverSettings.Default,
        NewKotlinTypeChecker.Default,
        JavaTypeEnhancementState.DISABLED_JSR_305,
        object : JavaModuleAnnotationsProvider {
            override fun getAnnotationsForModuleOwnerOfClass(classId: ClassId): List<JavaAnnotation>? = null
        }
    )
}

private fun createDeserializationComponentsForJava(
    moduleDescriptor: ModuleDescriptor,
    storageManager: StorageManager,
    notFoundClasses: NotFoundClasses,
    lazyJavaPackageFragmentProvider: LazyJavaPackageFragmentProvider,
    kotlinClassFinder: KotlinClassFinder,
    deserializedDescriptorResolver: DeserializedDescriptorResolver
): DeserializationComponentsForJava {
    val javaClassDataFinder = JavaClassDataFinder(kotlinClassFinder, deserializedDescriptorResolver)
    val binaryClassAnnotationAndConstantLoader = BinaryClassAnnotationAndConstantLoaderImpl(
        moduleDescriptor, notFoundClasses, storageManager, kotlinClassFinder
    )
    return DeserializationComponentsForJava(
        storageManager, moduleDescriptor, DeserializationConfiguration.Default, javaClassDataFinder,
        binaryClassAnnotationAndConstantLoader, lazyJavaPackageFragmentProvider, notFoundClasses,
        CustomErrorReporter, LookupTracker.DO_NOTHING, ContractDeserializer.DEFAULT, NewKotlinTypeChecker.Default
    )
}
