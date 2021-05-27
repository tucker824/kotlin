/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.components

import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtClassOrObjectSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtEnumEntrySymbol
import org.jetbrains.kotlin.psi.KtFile

enum class ShortenOption {
    /** Skip shortening references to this symbol. */
    DO_NOT_SHORTEN,

    /** Only shorten references to this symbol if it's already imported in the file. Otherwise, leave it as it is. */
    SHORTEN_IF_ALREADY_IMPORTED,

    /** Shorten references to this symbol and import it into the file. */
    SHORTEN_AND_IMPORT,

    /** Shorten references to this symbol and import this symbol and all its sibling symbols with on-demand import (i.e. star import). */
    SHORTEN_AND_IMPORT_ALL_IN_PARENT
}

abstract class KtReferenceShortener : KtAnalysisSessionComponent() {
    abstract fun collectShortenings(
        file: KtFile,
        selection: TextRange,
        classShortenOption: (KtClassOrObjectSymbol) -> ShortenOption,
        callableShortenOption: (KtCallableSymbol) -> ShortenOption
    ): ShortenCommand
}

interface KtReferenceShortenerMixIn : KtAnalysisSessionMixIn {
    fun collectPossibleReferenceShortenings(
        file: KtFile,
        selection: TextRange = file.textRange,
        classShortenOption: (KtClassOrObjectSymbol) -> ShortenOption = {
            if (it.classIdIfNonLocal?.isNestedClass == true) {
                ShortenOption.SHORTEN_IF_ALREADY_IMPORTED
            } else {
                ShortenOption.SHORTEN_AND_IMPORT
            }
        },
        callableShortenOption: (KtCallableSymbol) -> ShortenOption = { symbol ->
            if (symbol is KtEnumEntrySymbol) ShortenOption.DO_NOT_SHORTEN
            else ShortenOption.SHORTEN_AND_IMPORT
        }
    ): ShortenCommand =
        analysisSession.referenceShortener.collectShortenings(file, selection, classShortenOption, callableShortenOption)
}

interface ShortenCommand {
    fun invokeShortening()
    val isEmpty: Boolean
}