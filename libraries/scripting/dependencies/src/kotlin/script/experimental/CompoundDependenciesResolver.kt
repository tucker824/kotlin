/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental

import org.jetbrains.kotlin.script.util.resolvers.experimental.*
import java.io.File
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptDiagnostic

class CompoundDependenciesResolver(private val resolvers: List<GenericDependenciesResolver>) : GenericDependenciesResolver() {

    constructor(vararg resolvers: GenericDependenciesResolver) : this(resolvers.toList())

    override fun accepts(artifactCoordinates: GenericArtifactCoordinates): Boolean {
        return resolvers.any { it.accepts(artifactCoordinates) }
    }

    override fun accepts(repositoryCoordinates: GenericRepositoryCoordinates): Boolean {
        return resolvers.any { it.accepts(repositoryCoordinates) }
    }

    override fun addRepository(repositoryCoordinates: GenericRepositoryCoordinates) {
        if (resolvers.count { it.tryAddRepository(repositoryCoordinates) } == 0)
            throw Exception("Failed to detect repository type: ${repositoryCoordinates.string}")
    }

    override fun resolve(artifactCoordinates: GenericArtifactCoordinates): ResultWithDiagnostics<Iterable<File>> {

        val reports = mutableListOf<ScriptDiagnostic>()

        for (resolver in resolvers) {
            if (resolver.accepts(artifactCoordinates)) {
                when (val resolveResult = resolver.resolve(artifactCoordinates)) {
                    is ResultWithDiagnostics.Failure -> reports.addAll(resolveResult.reports)
                    else -> return resolveResult
                }
            }
        }
        return ResultWithDiagnostics.Failure(reports)
    }

}