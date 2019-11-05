/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.script

import com.intellij.openapi.application.impl.LaterInvocator
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.containers.HashSetQueue
import org.jetbrains.kotlin.idea.core.script.ScriptConfigurationManager
import org.jetbrains.kotlin.idea.core.script.applySuggestedScriptConfiguration
import org.jetbrains.kotlin.idea.core.script.configuration.utils.backgroundExecutorNewTaskHook
import org.jetbrains.kotlin.idea.core.script.configuration.utils.psiModificationStampHook
import org.jetbrains.kotlin.idea.core.script.configuration.utils.rootsIndexerTransaction
import org.jetbrains.kotlin.idea.core.script.configuration.utils.testScriptConfigurationNotification
import org.jetbrains.kotlin.psi.KtFile

class ScriptConfigurationLoadingTest : AbstractScriptConfigurationTest() {
    val backgroundQueue = HashSetQueue<BackgroundTask>()
    private lateinit var manager: ScriptConfigurationManager

    class BackgroundTask(val file: VirtualFile, val actions: () -> Unit) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as BackgroundTask

            if (file != other.file) return false

            return true
        }

        override fun hashCode(): Int {
            return file.hashCode()
        }
    }

    override fun setUp() {
        super.setUp()
        backgroundExecutorNewTaskHook = { file, actions ->
            backgroundQueue.add(BackgroundTask(file, actions))
        }
        testScriptConfigurationNotification = true
        psiModificationStampHook = ::getPsiModificationStamp

        configureScriptFile("idea/testData/script/definition/loading/async/")
        manager = ServiceManager.getService(project, ScriptConfigurationManager::class.java)
    }

    override fun tearDown() {
        super.tearDown()
        backgroundExecutorNewTaskHook = null
        testScriptConfigurationNotification = false
        psiModificationStampHook = null
    }

    override fun loadScriptConfigurationSynchronously(script: VirtualFile) {
        // do nothings
    }

    private val ktFile: KtFile
        get() = myFile as KtFile

    private val virtualFile
        get() = myFile.virtualFile

    private var modificationStamp = 0L

    fun getPsiModificationStamp(file: KtFile): Long =
        when (file.virtualFile) {
            virtualFile -> modificationStamp
            else -> file.modificationStamp
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

    private fun assertAppliedConfiguration(sequenceNumber: Int) {
        val secondConfiguration = manager.getConfiguration(ktFile)
        assertEquals(listOf("x${sequenceNumber}"), secondConfiguration?.defaultImports)
    }

    private fun makeChanges(stamp: Long = modificationStamp + 1) {
        modificationStamp = stamp
        manager.updater.ensureUpToDatedConfigurationSuggested(ktFile)
    }

    private fun assertAndApplySuggestedConfiguration() {
        assertTrue(virtualFile.applySuggestedScriptConfiguration(project))
    }

    private fun assertAndDoAllBackgroundTasks() {
        assertTrue(doAllBackgroundTasks())
    }

    private fun loadInitialConfiguration() {
        assertNull(manager.getConfiguration(ktFile))
        assertAndDoAllBackgroundTasks()
        assertAppliedConfiguration(1)
    }

    fun testSimple() {
        loadInitialConfiguration()

        makeChanges(1)
        assertAppliedConfiguration(1)
        assertAndDoAllBackgroundTasks()
        assertAppliedConfiguration(1)
        assertAndApplySuggestedConfiguration()
        assertAppliedConfiguration(2)
    }

    fun testConcurrentLoadingWhileInQueue() {
        loadInitialConfiguration()

        makeChanges(1)
        assertAppliedConfiguration(1)
        makeChanges(2)
        assertAndDoAllBackgroundTasks()
        assertAppliedConfiguration(1)
        assertAndApplySuggestedConfiguration()
        assertAppliedConfiguration(2)
    }

    fun testConcurrentLoadingWhileAnotherLoadInProgress() {
        // todo: suspend loading
    }

    fun testConcurrentLoadingWhileNotApplied() {
        loadInitialConfiguration()

        makeChanges(1)
        assertAppliedConfiguration(1)
        assertAndDoAllBackgroundTasks()
        assertAppliedConfiguration(1)

        // we have loaded and not applied configuration (loading 2)
        // let's invalidate file again and check that loading will occur (loading 3)

        makeChanges(2)
        assertAndDoAllBackgroundTasks()
        assertAppliedConfiguration(1)

        assertAndApplySuggestedConfiguration()
        assertAppliedConfiguration(3)
    }

    fun testConcurrentLoadingWhileNotApplied2() {
        loadInitialConfiguration()

        makeChanges(1)
        assertAppliedConfiguration(1)
        assertAndDoAllBackgroundTasks()
        assertAppliedConfiguration(1)

        // we have loaded and not applied configuration (loading 2)
        // let's invalidate file and change it back
        // and check that loading will NOT occur

        makeChanges(2)
        makeChanges(1)

        assertAndDoAllBackgroundTasks()
        assertAppliedConfiguration(1)

        assertAndApplySuggestedConfiguration()
        assertAppliedConfiguration(2)
    }

    // todo: test change back
    // todo: test reports

    // todo: test indexing new roots
    // todo: test fs caching
    // todo: test gradle specific logic

    // todo: test not running loading for usages search
}