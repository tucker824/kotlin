/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.script.util.resolvers

import com.amazonaws.services.cloudfront.model.InvalidArgumentException
import org.jetbrains.kotlin.script.util.Repository
import org.jetbrains.kotlin.script.util.resolvers.experimental.*
import java.io.File
import java.lang.Exception
import java.net.MalformedURLException
import java.net.URL
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.asSuccess
import kotlin.script.experimental.api.makeFailureResult

class DirectResolver : GenericRepositoryWithBridge {

    private fun makeResolveFailureResult(location: String, message: String) = ResolveArtifactResult.Failure(listOf(ResolveAttemptFailure(location, message)))

    override fun resolve(artifactCoordinates: GenericArtifactCoordinates): ResolveArtifactResult {
        if(!accepts(artifactCoordinates)) throw InvalidArgumentException("Invalid arguments: $artifactCoordinates")
        val file = File(artifactCoordinates.string)
        if(!file.exists()) return makeResolveFailureResult(file.canonicalPath, "File doesn't exist")
        if(!file.isFile && !file.isDirectory) return makeResolveFailureResult(file.canonicalPath, "Path is neither file nor directory")
        return ResolveArtifactResult.Success(listOf(file))
    }

    override fun addRepository(repositoryCoordinates: GenericRepositoryCoordinates) =
        throw Exception("DirectResolver doesn't support adding repositories")

    override fun accepts(repositoryCoordinates: GenericRepositoryCoordinates): Boolean = false

    override fun accepts(artifactCoordinates: GenericArtifactCoordinates): Boolean =
        !artifactCoordinates.string.isBlank() && !artifactCoordinates.string.contains(':')
}

class FlatLibDirectoryResolver(vararg paths: File) : GenericRepositoryWithBridge {

    override fun addRepository(repositoryCoordinates: GenericRepositoryCoordinates) {
        val repoDir = repositoryCoordinates.file ?: throw Exception("Invalid repository location: '${repositoryCoordinates.string}'")
        localRepos.add(repoDir)
    }

    override fun resolve(artifactCoordinates: GenericArtifactCoordinates): ResolveArtifactResult {
        if(!accepts(artifactCoordinates)) throw Exception("Path is empty")

        val resolveAttempts = mutableListOf<ResolveAttemptFailure>()

        val path = artifactCoordinates.string
        for (repo in localRepos) {
            // TODO: add coordinates and wildcard matching
            val file = File(repo, path)
            when {
                !file.exists() -> resolveAttempts.add(ResolveAttemptFailure(file.canonicalPath, "File not exists"))
                !file.isFile && !file.isDirectory -> resolveAttempts.add(ResolveAttemptFailure(file.canonicalPath, "Path is neither file nor directory"))
                else -> return ResolveArtifactResult.Success(listOf(file))
            }
        }
        return ResolveArtifactResult.Failure(resolveAttempts)
    }

    override fun accepts(artifactCoordinates: GenericArtifactCoordinates): Boolean {
        return artifactCoordinates.string.takeUnless(String::isBlank)?.let { path ->
            localRepos.any { File(it, path).exists() }
        } ?: false
    }

    override fun accepts(repositoryCoordinates: GenericRepositoryCoordinates): Boolean = repositoryCoordinates.file != null

    private val localRepos = arrayListOf<File>()

    init {
        for (path in paths) {
            if (!path.exists() || !path.isDirectory) throw IllegalArgumentException("Invalid flat lib directory repository path '$path'")
        }
        localRepos.addAll(paths)
    }

    companion object {
        fun tryCreate(annotation: Repository): FlatLibDirectoryResolver? = tryCreate(
            BasicRepositoryCoordinates(
                annotation.url.takeUnless(String::isBlank) ?: annotation.value, annotation.id.takeUnless(String::isBlank)
            )
        )

        fun tryCreate(repositoryCoordinates: GenericRepositoryCoordinates): FlatLibDirectoryResolver? =
            repositoryCoordinates.file?.let { FlatLibDirectoryResolver(it) }
    }
}

internal fun String.toRepositoryUrlOrNull(): URL? =
    try {
        URL(this)
    } catch (_: MalformedURLException) {
        null
    }

internal fun String.toRepositoryFileOrNull(): File? =
    File(this).takeIf { it.exists() && it.isDirectory }
