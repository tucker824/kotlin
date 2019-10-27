/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script.configuration.loader

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.core.script.configuration.cache.CachedConfigurationSnapshot

interface ScriptConfigurationLoadingContext {
    fun getCachedConfiguration(file: VirtualFile): CachedConfigurationSnapshot?

    /**
     * Save [newResult] for [file] into caches and update highlighting.
     *
     * @sample DefaultScriptConfigurationLoader.loadDependencies
     */
    fun suggestNewConfiguration(
        file: VirtualFile,
        newResult: LoadedScriptConfiguration
    )

    /**
     * Save [newResult] for [file] into caches and update highlighting.
     *
     * @sample ScriptConfigurationFileAttributeCache.loadDependencies
     */
    fun saveNewConfiguration(
        file: VirtualFile,
        newResult: LoadedScriptConfiguration
    )
}