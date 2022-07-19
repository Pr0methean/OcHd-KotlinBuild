package io.github.pr0methean.ochd

import io.github.pr0methean.ochd.tasks.OutputTask
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import java.util.*
import java.util.Collections.newSetFromMap
import java.util.Collections.synchronizedSet

/**
 * A class that launches a collection of OutputTask instances, waits for them all to complete, and also ensures they
 * each become unreachable on completion.
 */
@Suppress("DeferredResultUnused")
class CompletionHandler(tasks: Iterable<OutputTask>, val scope: CoroutineScope) {
    private val runningTasks: MutableSet<OutputTask> = synchronizedSet(newSetFromMap(IdentityHashMap()))
    init {
        runningTasks.addAll(tasks)
        runningTasks.forEach {
            scope.async {it.run(::remove)}
        }
    }
    private val allFinished = CompletableDeferred<Unit>()

    private fun remove(task: OutputTask) {
        runningTasks.remove(task)
        if (runningTasks.isEmpty()) {
            allFinished.complete(Unit)
        }
    }

    suspend fun awaitAllFinished() = allFinished.await()
}