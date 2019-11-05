/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.script

import com.intellij.openapi.application.impl.LaterInvocator
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.containers.HashSetQueue
import junit.framework.TestCase
import org.jetbrains.kotlin.idea.core.script.ScriptConfigurationManager
import org.jetbrains.kotlin.idea.core.script.applySuggestedScriptConfiguration
import org.jetbrains.kotlin.idea.core.script.configuration.utils.backgroundExecutorNewTaskHook
import org.jetbrains.kotlin.idea.core.script.configuration.utils.rootsIndexerTransaction
import org.jetbrains.kotlin.idea.core.script.configuration.utils.testScriptConfigurationNotification
import org.jetbrains.kotlin.psi.KtFile
import kotlin.test.assertNotEquals

class ScriptConfigurationLoadingTest : AbstractScriptConfigurationTest() {
    val backgroundQueue = HashSetQueue<BackgroundTask>()
    private lateinit var manager: ScriptConfigurationManager

    class BackgroundTask(val file: VirtualFile, val actions: () -> Unit)

    override fun setUp() {
        super.setUp()
        backgroundExecutorNewTaskHook = { file, actions ->
            backgroundQueue.add(BackgroundTask(file, actions))
        }
        testScriptConfigurationNotification = true

        configureScriptFile("idea/testData/script/definition/loading/async/")
        manager = ServiceManager.getService(project, ScriptConfigurationManager::class.java)
    }

    override fun tearDown() {
        super.tearDown()
        backgroundExecutorNewTaskHook = null
        testScriptConfigurationNotification = false
    }

    override fun loadScriptConfigurationSynchronously(script: VirtualFile) {
        // do nothings
    }

    private fun doAllBackgroundTasks(): Boolean {
        if (backgroundQueue.isEmpty()) return false

        val copy = backgroundQueue.toList()
        backgroundQueue.clear()

        rootsIndexerTransaction {
            copy.forEach {
                it.actions()
            }
        }

        LaterInvocator.ensureFlushRequested()
        LaterInvocator.dispatchPendingFlushes()

        return true
    }

    private val ktFile: KtFile
        get() = myFile as KtFile

    private val virtualFile
        get() = myFile.virtualFile

    private fun assertAppliedConfiguration(sequenceNumber: Int) {
        val secondConfiguration = manager.getConfiguration(ktFile)
        assertEquals(listOf("x${sequenceNumber}"), secondConfiguration?.defaultImports)
    }

    private fun loadInitialConfiguration() {
        assertNull(manager.getConfiguration(ktFile))
        assertTrue(doAllBackgroundTasks())
        assertAppliedConfiguration(1)
    }

    fun testSimple() {
        loadInitialConfiguration()

        manager.updater.forceConfigurationReload(ktFile)
        assertAppliedConfiguration(1)
        assertTrue(doAllBackgroundTasks())
        assertAppliedConfiguration(1)
        assertTrue(virtualFile.applySuggestedScriptConfiguration(project))
        assertAppliedConfiguration(2)
    }

    fun testConcurrentLoadingWhileInQueue() {
        loadInitialConfiguration()

        manager.updater.forceConfigurationReload(ktFile)
        assertAppliedConfiguration(1)
        manager.updater.forceConfigurationReload(ktFile)
        assertTrue(doAllBackgroundTasks())
        assertAppliedConfiguration(1)
        assertTrue(virtualFile.applySuggestedScriptConfiguration(project))
        assertAppliedConfiguration(2)
    }

    fun testConcurrentLoadingWhileAnotherLoadInProgress() {
        // todo: suspend loading
    }

    fun testConcurrentLoadingWhileNotApplied() {
        loadInitialConfiguration()

        manager.updater.forceConfigurationReload(ktFile)
        assertAppliedConfiguration(1)
        assertTrue(doAllBackgroundTasks())
        assertAppliedConfiguration(1)
        manager.updater.forceConfigurationReload(ktFile)
        assertTrue(doAllBackgroundTasks())
        assertAppliedConfiguration(1)
        assertTrue(virtualFile.applySuggestedScriptConfiguration(project))
        assertAppliedConfiguration(2)
    }

    // todo: test change back
    // todo: test reports

    // todo: test indexing new roots
    // todo: test fs caching
    // todo: test gradle specific logic
}