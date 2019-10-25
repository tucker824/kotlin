/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental

import java.io.File
import kotlin.script.experimental.api.ResultWithDiagnostics

class DirectDependenciesResolver : GenericDependenciesResolver() {

    override fun resolve(artifactCoordinates: String): ResultWithDiagnostics<Iterable<File>> {
        if(!acceptsArtifact(artifactCoordinates)) throw IllegalArgumentException("Invalid arguments: $artifactCoordinates")
        val file = File(artifactCoordinates)
        if(!file.exists()) return makeResolveFailureResult("File '${file.canonicalPath}' doesn't exist")
        if(!file.isFile && !file.isDirectory) return makeResolveFailureResult("Path '${file.canonicalPath}' is neither file nor directory")
        return ResultWithDiagnostics.Success(listOf(file))
    }

    override fun addRepository(repositoryCoordinates: String) =
        throw Exception("DirectDependenciesResolver doesn't support adding repositories")

    override fun acceptsRepository(repositoryCoordinates: String): Boolean = false

    override fun acceptsArtifact(artifactCoordinates: String): Boolean =
        !artifactCoordinates.isBlank() && !artifactCoordinates.contains(':')
}