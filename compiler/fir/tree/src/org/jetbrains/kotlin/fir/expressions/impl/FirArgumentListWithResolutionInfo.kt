/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.impl

import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor

interface FirArgumentListWithResolutionInfo {
    val mapping: LinkedHashMap<FirExpression, FirValueParameter>
    val typeParameterSubstitutor: ConeSubstitutor
}