/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.incremental

import org.jetbrains.kotlin.build.report.BuildReporter
import org.jetbrains.kotlin.build.report.ICReporter
import org.jetbrains.kotlin.build.report.metrics.BuildAttribute
import org.jetbrains.kotlin.build.report.metrics.BuildMetrics
import org.jetbrains.kotlin.build.report.metrics.BuildMetricsReporter
import org.jetbrains.kotlin.build.report.metrics.BuildTime
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.incremental.*
import java.io.File

class ClasspathSnapshotDiffComputer(
    val current: ClasspathSnapshot,
    val previous: ClasspathSnapshot
) {

    // INCR_KOTLIN_COMPILE_BOOKMARK
    // Step 2b [Gradle side]: Compute classpath snapshot changes
    fun getChanges(): DirtyData? {
        val changesCollector = QuittableChangesCollector()
        collectChanges(changesCollector)

        return if (!changesCollector.hasQuit()) {
            val (dirtyLookupSymbols, dirtyClassFqNames, _) =
                changesCollector.changesCollector.getDirtyData(emptyList(), NoOpBuildReport)
            DirtyData(dirtyLookupSymbols, dirtyClassFqNames)
        } else {
            null
        }
    }

    private fun collectChanges(changesCollector: QuittableChangesCollector) {
        // TODO Handle added, removed, reordered classpath entries
        if (current.classpathEntrySnapshots.size == previous.classpathEntrySnapshots.size) {
            for (index in current.classpathEntrySnapshots.indices) {
                ClasspathEntrySnapshotDiffComputer(current.classpathEntrySnapshots[index], previous.classpathEntrySnapshots[index])
                    .collectChanges(changesCollector)
            }
        } else {
            changesCollector.quit("Added/removed classpath entries are not yet handled.")
        }
    }
}

class ClasspathEntrySnapshotDiffComputer(
    val current: ClasspathEntrySnapshot,
    val previous: ClasspathEntrySnapshot
) {

    fun collectChanges(changesCollector: QuittableChangesCollector) {
        val currentClasses = current.classSnapshots.keys.sorted()
        val previousClasses = previous.classSnapshots.keys.sorted()

        // TODO Handle added and removed classes
        if (currentClasses == previousClasses) {
            for (key in currentClasses) {
                ClassSnapshotDiffComputer(current.classSnapshots[key]!!, previous.classSnapshots[key]!!).collectChanges(changesCollector)
            }
        } else {
            changesCollector.quit("Added/removed classes are not yet handled")
        }
    }
}

class ClassSnapshotDiffComputer(
    val current: ClassSnapshot,
    val previous: ClassSnapshot
) {

    fun collectChanges(changesCollector: QuittableChangesCollector) {
        if (current.javaClass != previous.javaClass) {
            // TODO Handle this case
            changesCollector.quit("Current and previous snapshots differ in types. Current: ${current.javaClass.name}. Previous: ${previous.javaClass.name}")
            return
        }

        when (current) {
            is KotlinClassSnapshot -> collectKotlinClassChanges(current, previous as KotlinClassSnapshot, changesCollector)
            is JavaClassSnapshot -> collectJavaClassChanges(current, previous as JavaClassSnapshot, changesCollector)
            is FailedClassSnapshot -> changesCollector.quit("Current and previous snapshot were not created.")
        }
    }

    private fun collectKotlinClassChanges(
        current: KotlinClassSnapshot,
        previous: KotlinClassSnapshot,
        changesCollector: QuittableChangesCollector
    ) {
        check(current.packageFqName == previous.packageFqName)
        check(!current.isEmpty() || !previous.isEmpty()) {
            "No current and previous data found."
        }
        if (current.protoMapValue != null || previous.protoMapValue != null) {
            // TODO Collect more changes (e.g., constants and inline functions). See ChangesCollector API for all possible cases.
            changesCollector.collectProtoChanges(
                oldData = previous.protoMapValue?.toProtoData(previous.packageFqName),
                newData = current.protoMapValue?.toProtoData(current.packageFqName)
            )
        }
    }

    private fun collectJavaClassChanges(
        current: JavaClassSnapshot,
        previous: JavaClassSnapshot,
        changesCollector: QuittableChangesCollector
    ) {
        changesCollector.collectProtoChanges(
            previous.serializedJavaClass.toProtoData(),
            current.serializedJavaClass.toProtoData(),
            collectAllMembersForNewClass = true
        )
    }
}

/** Similar to [ChangesCollector] but allows quitting early if the results won't be used in the end. */
class QuittableChangesCollector {
    val changesCollector: ChangesCollector = ChangesCollector()
    private var hasQuit: Boolean = false
    private var reason: String? = null

    fun collectProtoChanges(oldData: ProtoData?, newData: ProtoData?, collectAllMembersForNewClass: Boolean = false) {
        if (hasQuit) {
            return
        }
        changesCollector.collectProtoChanges(oldData, newData, collectAllMembersForNewClass)
    }

    fun hasQuit() = hasQuit

    fun quit(reason: String) {
        hasQuit = true
        this.reason = reason
    }
}

// Temporary - remove later
object NoOpBuildReport : BuildReporter(NoOpICReporter, NoOpBuildMetricsReporter)

// Temporary - remove later
object NoOpICReporter : ICReporter {
    override fun report(message: () -> String) {}
    override fun reportVerbose(message: () -> String) {}
    override fun reportCompileIteration(incremental: Boolean, sourceFiles: Collection<File>, exitCode: ExitCode) {}
    override fun reportMarkDirtyClass(affectedFiles: Iterable<File>, classFqName: String) {}
    override fun reportMarkDirtyMember(affectedFiles: Iterable<File>, scope: String, name: String) {}
    override fun reportMarkDirty(affectedFiles: Iterable<File>, reason: String) {}
}

// Temporary - remove later
object NoOpBuildMetricsReporter : BuildMetricsReporter {
    override fun startMeasure(metric: BuildTime, startNs: Long) {}
    override fun endMeasure(metric: BuildTime, endNs: Long) {}
    override fun addAttribute(attribute: BuildAttribute) {}
    override fun getMetrics(): BuildMetrics = BuildMetrics()
    override fun addMetrics(metrics: BuildMetrics?) {}
}