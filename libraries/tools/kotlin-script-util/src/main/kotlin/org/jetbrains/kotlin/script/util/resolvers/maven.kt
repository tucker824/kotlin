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

@file:DependsOn("org.funktionale:funktionale:0.9.6")

package org.jetbrains.kotlin.script.util.resolvers

import com.jcabi.aether.Aether
import org.jetbrains.kotlin.script.util.DependsOn
import org.jetbrains.kotlin.script.util.resolvers.experimental.*
import org.sonatype.aether.repository.RemoteRepository
import org.sonatype.aether.resolution.DependencyResolutionException
import org.sonatype.aether.util.artifact.DefaultArtifact
import org.sonatype.aether.util.artifact.JavaScopes
import java.io.File
import java.util.*
import kotlin.script.experimental.api.makeFailureResult

val mavenCentral = RemoteRepository("maven-central", "default", "https://repo.maven.apache.org/maven2/")

class MavenResolver : GenericRepositoryWithBridge {

    override fun accepts(artifactCoordinates: GenericArtifactCoordinates): Boolean =
        artifactCoordinates.mavenArtifact != null

    override fun accepts(repositoryCoordinates: GenericRepositoryCoordinates): Boolean {
        return repositoryCoordinates.url != null
    }

    // TODO: make robust
    val localRepo = File(File(System.getProperty("user.home")!!, ".m2"), "repository")

    val repos: ArrayList<RemoteRepository> = arrayListOf()

    private fun remoteRepositories() = if (repos.isEmpty()) arrayListOf(mavenCentral) else repos

    private fun allRepositories() = remoteRepositories().map { it.url!!.toString() } + localRepo.toString()

    private fun String?.isValidParam() = this?.isNotBlank() ?: false

    private val GenericArtifactCoordinates.mavenArtifact : DefaultArtifact?
        get() {
            fun String?.nullIfBlank(): String? = this?.takeUnless(String::isBlank)

            return if (this is MavenArtifactCoordinates && (groupId.isValidParam() || artifactId.isValidParam())) {
                DefaultArtifact(
                    groupId.nullIfBlank(),
                    artifactId.nullIfBlank(),
                    null,
                    version.nullIfBlank()
                )
            } else {
                val coordinatesString = string
                if (coordinatesString.isValidParam() && coordinatesString.count { it == ':' } == 2) {
                    DefaultArtifact(coordinatesString)
                } else {
                    null
                }
            }
        }

    override fun resolve(artifactCoordinates: GenericArtifactCoordinates): ResolveArtifactResult {

        val artifactId = artifactCoordinates.mavenArtifact!!

        return try {
            val deps = Aether(remoteRepositories(), localRepo).resolve(artifactId, JavaScopes.RUNTIME)
            if (deps != null)
                ResolveArtifactResult.Success(deps.map { it.file })
            else {
                ResolveArtifactResult.Failure(allRepositories().map { ResolveAttemptFailure(it, "$artifactId not found") })
            }
        } catch (e: DependencyResolutionException) {
            ResolveArtifactResult.Failure(allRepositories().map { ResolveAttemptFailure(it, "failed to resolve dependencies") })
        }
    }

    override fun addRepository(repositoryCoordinates: GenericRepositoryCoordinates) {
        val url = repositoryCoordinates.url ?: throw Exception("Invalid Maven repository URL: ${repositoryCoordinates.string}")

        repos.add(
            RemoteRepository(
                if (repositoryCoordinates.name.isValidParam()) repositoryCoordinates.name else url.host,
                "default",
                url.toString()
            )
        )
    }
}
