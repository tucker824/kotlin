/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.test

import junit.framework.TestCase
import org.junit.Assert
import java.io.File
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.script.experimental.CompoundDependenciesResolver
import kotlin.script.experimental.DirectDependenciesResolver
import kotlin.script.experimental.FlatLibDirectoryDependenciesResolver
import kotlin.script.experimental.GenericDependenciesResolver
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.valueOrThrow

@ExperimentalContracts
fun <T> assertIsFailure(r: ResultWithDiagnostics<T>) {
    contract {
        returns() implies (r is ResultWithDiagnostics.Failure)
    }

    Assert.assertTrue(r is ResultWithDiagnostics.Failure)
}

@ExperimentalContracts
fun <T> assertIsSuccess(r: ResultWithDiagnostics<T>){
    contract {
        returns() implies (r is ResultWithDiagnostics.Success<T>)
    }

    TestCase.assertTrue(r is ResultWithDiagnostics.Success<T>)
}

@ExperimentalContracts
class ResolversTest : TestCase() {

    fun <T> withTempFile(body: (file: File) -> T): T {
        createTempFile()
        val file = createTempFile()
        file.deleteOnExit()
        try {
            return body(file)
        } finally {
            file.delete()
        }
    }

    fun getNonExistingFile() = withTempFile {it }.also { assertFalse(it.exists()) }

    fun GenericDependenciesResolver.assertNotResolve(expectedReportsCount: Int, path: String){
        val result = resolve(path)
        assertIsFailure(result)
        assertEquals(expectedReportsCount, result.reports.count())
    }

    fun GenericDependenciesResolver.assertAcceptsArtifact(path: String)
        = assertTrue(acceptsArtifact(path))

    fun GenericDependenciesResolver.assertNotAcceptsArtifact(path: String)
            = assertFalse(acceptsArtifact(path))

    fun GenericDependenciesResolver.assertAcceptsRepository(path: String)
            = assertTrue(acceptsRepository(path))

    fun GenericDependenciesResolver.assertResolve(expected: File, path: String){

        assertTrue(acceptsArtifact(path))

        val result = resolve(path)
        assertIsSuccess(result)

        val value = result.valueOrThrow()
        assertEquals(1, value.count())
        assertEquals(expected.canonicalPath, value.first().canonicalPath)
    }

    fun testDirectResolver() {
        withTempFile { file ->

            val resolver = DirectDependenciesResolver()

            assertFalse(resolver.acceptsRepository(file.canonicalPath))
            resolver.assertAcceptsArtifact(file.canonicalPath)

            resolver.assertResolve(file, file.canonicalPath)
        }
    }

    fun testDirectResolverFail() {
        val file = getNonExistingFile()

        val resolver = DirectDependenciesResolver()

        resolver.assertAcceptsArtifact(file.canonicalPath)
        resolver.assertNotResolve(1, file.canonicalPath)
    }

    fun testFlatLibDirectoryResolver() {
        withTempFile { file ->

            val resolver = FlatLibDirectoryDependenciesResolver()

            val dir = file.parent!!
            resolver.assertAcceptsRepository(dir)
            resolver.addRepository(dir)

            resolver.assertResolve(file, file.name)
        }
    }

    fun testFlatLibDirectoryResolverFail() {
        val file = getNonExistingFile()

        val resolver = FlatLibDirectoryDependenciesResolver()
        resolver.assertNotAcceptsArtifact(file.path)

        resolver.addRepository(file.parent)

        resolver.assertAcceptsArtifact(file.path)
        resolver.assertAcceptsArtifact(file.canonicalPath)
        resolver.assertNotResolve(1, file.path)
        resolver.assertNotResolve(1, file.canonicalPath)

        resolver.addRepository(file.parentFile.parent)
        resolver.assertNotResolve(2, file.path)
    }

    fun testCompoundResolver() {
        withTempFile { file ->
            val resolver = CompoundDependenciesResolver(DirectDependenciesResolver(), FlatLibDirectoryDependenciesResolver())

            resolver.assertAcceptsArtifact(file.name)
            resolver.assertNotResolve(1, file.name)

            val dir = file.parent!!
            resolver.assertAcceptsRepository(dir)
            resolver.addRepository(dir)

            resolver.assertResolve(file, file.name)
        }
    }

    fun testCompoundResolverFail() {
        val file = getNonExistingFile()

        val resolver = CompoundDependenciesResolver(DirectDependenciesResolver(), FlatLibDirectoryDependenciesResolver())

        resolver.assertAcceptsArtifact(file.name)
        resolver.assertNotResolve(1, file.name)

        resolver.addRepository(file.parent)
        resolver.assertNotResolve(2, file.name)

        resolver.assertNotResolve(2, file.canonicalPath)
    }
}
