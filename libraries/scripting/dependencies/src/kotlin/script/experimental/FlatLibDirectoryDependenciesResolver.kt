/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental

import java.io.File
import kotlin.script.experimental.api.ResultWithDiagnostics

class FlatLibDirectoryDependenciesResolver(vararg paths: File) : GenericDependenciesResolver() {

    private fun String.toRepositoryFileOrNull(): File? =
        File(this).takeIf { it.exists() && it.isDirectory }

    fun String.toFilePath() = (this.toRepositoryUrlOrNull()?.takeIf { it.protocol == "file" }?.path ?: this).toRepositoryFileOrNull()

    override fun addRepository(repositoryCoordinates: String) {
        val repoDir = repositoryCoordinates.toFilePath() ?: throw Exception("Invalid repository location: '${repositoryCoordinates}'")
        localRepos.add(repoDir)
    }

    override fun resolve(artifactCoordinates: String): ResultWithDiagnostics<Iterable<File>> {
        if(!acceptsArtifact(artifactCoordinates)) throw Exception("Path is empty")

        val resolveAttempts = mutableListOf<String>()

        val path = artifactCoordinates
        for (repo in localRepos) {
            // TODO: add coordinates and wildcard matching
            val file = File(repo, path)
            when {
                !file.exists() -> resolveAttempts.add("File '${file.canonicalPath}' not exists")
                !file.isFile && !file.isDirectory -> resolveAttempts.add("Path '${file.canonicalPath}' is neither file nor directory")
                else -> return ResultWithDiagnostics.Success(listOf(file))
            }
        }
        return makeResolveFailureResult(resolveAttempts)
    }

    override fun acceptsArtifact(artifactCoordinates: String): Boolean {
        return artifactCoordinates.takeUnless(String::isBlank)?.let { path ->
            localRepos.any { File(it, path).exists() }
        } ?: false
    }

    override fun acceptsRepository(repositoryCoordinates: String): Boolean = repositoryCoordinates.toFilePath() != null

    private val localRepos = arrayListOf<File>()

    init {
        for (path in paths) {
            if (!path.exists() || !path.isDirectory) throw IllegalArgumentException("Invalid flat lib directory repository path '$path'")
        }
        localRepos.addAll(paths)
    }
}