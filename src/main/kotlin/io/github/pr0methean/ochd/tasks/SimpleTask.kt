package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.tasks.caching.TaskCache
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async

/**
 * Task that either has no other tasks as input, or manages its input in subclass fields.
 */
abstract class SimpleTask<T>(name: String, cache: TaskCache<T>) : AbstractTask<T>(
    name, cache
) {
    abstract suspend fun perform(): T

    override suspend fun createCoroutineAsync(): Deferred<Result<T>> {
        return getCoroutineScope().async (start = CoroutineStart.LAZY) {
            val result = runCatching {perform()}
            emit(result)
            result
        }
    }
}
