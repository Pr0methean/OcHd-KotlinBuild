package io.github.pr0methean.ochd

import io.github.pr0methean.ochd.tasks.OutputTask
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import java.util.concurrent.atomic.AtomicInteger

/**
 * A class that launches a collection of OutputTask instances, waits for them all to complete, and also ensures they
 * each become unreachable on completion.
 */
@Suppress("DeferredResultUnused")
class CompletionHandler(tasks: Collection<OutputTask>, scope: CoroutineScope) {
    private val remainingTasks = AtomicInteger(tasks.size)
    init {
        tasks.forEach {
            scope.async {it.run(::onOneFinished)}
        }
    }
    private val allFinished = CompletableDeferred<Unit>()

    private fun onOneFinished() {
        if (remainingTasks.decrementAndGet() == 0) {
            allFinished.complete(Unit)
        }
    }

    suspend fun awaitAllFinished() = allFinished.await()
}