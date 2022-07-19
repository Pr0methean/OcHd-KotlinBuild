package io.github.pr0methean.ochd

import io.github.pr0methean.ochd.tasks.OutputTask
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger

/**
 * A class that launches a collection of OutputTask instances, waits for them all to complete, and also ensures they
 * each become unreachable on completion.
 */
@Suppress("DeferredResultUnused")
class CompletionHandler(val scope: CoroutineScope) {
    private val remainingTasks = AtomicInteger()
    @Volatile
    private var allAdded: Boolean = false

    fun add(task: OutputTask) {
        remainingTasks.getAndIncrement()
        scope.launch {task.run(::onOneFinished)}
    }

    fun onAllAdded() {
        allAdded = true
        if (remainingTasks.get() == 0) {
            allFinished.complete(Unit)
        }
    }

    private val allFinished = CompletableDeferred<Unit>()

    private fun onOneFinished() {
        if (remainingTasks.decrementAndGet() == 0 && allAdded) {
            allFinished.complete(Unit)
        }
    }

    suspend fun awaitAllFinished() = allFinished.await()
}