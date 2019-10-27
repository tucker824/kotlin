/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script.configuration

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiRecursiveElementVisitor
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.idea.core.script.configuration.cache.CachedConfigurationInputs
import org.jetbrains.kotlin.idea.core.script.configuration.loader.DefaultScriptConfigurationLoader
import org.jetbrains.kotlin.idea.core.script.configuration.loader.ScriptConfigurationLoadingContext
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtScriptInitializer
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinition

class GradleScriptConfigurationLoader(project: Project) : DefaultScriptConfigurationLoader(project) {
    private val useProjectImport = false // todo: registry key

    override fun shouldRunInBackground(scriptDefinition: ScriptDefinition): Boolean {
        return if (useProjectImport) false else super.shouldRunInBackground(scriptDefinition)
    }

    override fun loadDependencies(
        isFirstLoad: Boolean,
        virtualFile: VirtualFile,
        scriptDefinition: ScriptDefinition,
        context: ScriptConfigurationLoadingContext
    ): Boolean {
        if (!isGradleKotlinScript(virtualFile)) return false

        if (useProjectImport) {
            // do nothing, project import notification will be already showed
            // and configuration for gradle build scripts will be saved at the end of import
            // todo: use default configuration loader for out-of-project scripts?
            return true
        } else {
            super.loadDependencies(isFirstLoad, virtualFile, scriptDefinition, context)
            return true
        }
    }

    override fun getInputsStamp(file: KtFile): CachedConfigurationInputs {
        return getGradleScriptInputsStamp(project, file.virtualFile, file) ?: super.getInputsStamp(file)
    }
}

data class GradleKotlinScriptConfigurationInputs(
    val buildScriptAndPluginsSections: String,
    val relatedFilesModificationsTimestamp: Long
) : CachedConfigurationInputs {
    override fun isUpToDate(project: Project, file: VirtualFile, ktFile: KtFile?): Boolean {
        val actualStamp = getGradleScriptInputsStamp(project, file, ktFile)
        return actualStamp == this
    }
}

fun isGradleKotlinScript(virtualFile: VirtualFile) = virtualFile.name.endsWith(".gradle.kts")

private fun getGradleScriptInputsStamp(
    project: Project,
    file: VirtualFile,
    givenKtFile: KtFile? = null
): GradleKotlinScriptConfigurationInputs? {
    if (!isGradleKotlinScript(file)) return null

    return runReadAction {
        val ktFile = givenKtFile ?: PsiManager.getInstance(project).findFile(file) as? KtFile

        if (ktFile != null) {
            val result = StringBuilder()
            ktFile.script?.blockExpression
                ?.getChildrenOfType<KtScriptInitializer>()
                ?.forEach {
                    val call = it.children.singleOrNull() as? KtCallExpression
                    val callRef = call?.firstChild?.text
                    if (callRef == "buildscript" || callRef == "plugins") {
                        result.append(callRef)
                        val lambda = call.lambdaArguments.singleOrNull()
                        lambda?.accept(object : PsiRecursiveElementVisitor(false) {
                            override fun visitElement(element: PsiElement) {
                                super.visitElement(element)
                                when (element) {
                                    is PsiWhiteSpace -> if (element.text.contains("\n")) result.append("\n")
                                    is LeafPsiElement -> result.append(element.text)
                                }
                            }
                        })
                        result.append("\n")
                    }
                }

            GradleKotlinScriptConfigurationInputs(
                result.toString(),
                ServiceManager.getService(project, GradleRelatedFilesListener::class.java)?.lastModified
                    ?: Long.MIN_VALUE
            )
        } else null
    }
}

class GradleRelatedFilesListener {
    @Volatile
    var lastModified: Long = Long.MIN_VALUE
}