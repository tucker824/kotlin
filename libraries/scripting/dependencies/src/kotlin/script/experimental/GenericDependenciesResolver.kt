/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental

import org.jetbrains.kotlin.script.util.resolvers.experimental.BasicArtifactCoordinates
import org.jetbrains.kotlin.script.util.resolvers.experimental.BasicRepositoryCoordinates
import org.jetbrains.kotlin.script.util.resolvers.experimental.GenericArtifactCoordinates
import org.jetbrains.kotlin.script.util.resolvers.experimental.GenericRepositoryCoordinates
import java.io.File
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptDiagnostic

abstract class GenericDependenciesResolver {

    abstract fun accepts(repositoryCoordinates: GenericRepositoryCoordinates): Boolean
    abstract fun accepts(artifactCoordinates: GenericArtifactCoordinates): Boolean

    abstract fun resolve(artifactCoordinates: GenericArtifactCoordinates): ResultWithDiagnostics<Iterable<File>>
    abstract fun addRepository(repositoryCoordinates: GenericRepositoryCoordinates)

    protected fun makeResolveFailureResult(message: String) = makeResolveFailureResult(listOf(message))

    protected fun makeResolveFailureResult(messages: Iterable<String>) =
        ResultWithDiagnostics.Failure(messages.map { ScriptDiagnostic(it, ScriptDiagnostic.Severity.WARNING)})
}

fun GenericDependenciesResolver.tryResolve(artifactCoordinates: GenericArtifactCoordinates): Iterable<File>? =
    if (accepts(artifactCoordinates)) resolve(artifactCoordinates).let {
        when (it) {
            is ResultWithDiagnostics.Success -> it.value
            else -> null
        }
    } else null

fun GenericDependenciesResolver.tryAddRepository(repositoryCoordinates: GenericRepositoryCoordinates) =
    if (accepts(repositoryCoordinates)) {
        addRepository(repositoryCoordinates)
        true
    } else false

fun GenericDependenciesResolver.tryResolve(artifactCoordinates: String): Iterable<File>? =
    tryResolve(BasicArtifactCoordinates(artifactCoordinates))

fun GenericDependenciesResolver.tryAddRepository(repositoryCoordinates: String, id: String? = null): Boolean =
    tryAddRepository(BasicRepositoryCoordinates(repositoryCoordinates, id))