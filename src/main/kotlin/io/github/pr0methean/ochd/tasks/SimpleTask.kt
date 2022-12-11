package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.tasks.caching.DeferredTaskCache
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlin.coroutines.CoroutineContext

/**
 * Task that either has no other tasks as input, or manages its input in subclass fields.
 */
abstract class SimpleTask<T>(name: String, cache: DeferredTaskCache<T>, ctx: CoroutineContext) : AbstractTask<T>(
    name, cache, ctx
) {
    abstract suspend fun perform(): T

    override fun createCoroutineAsync(): Deferred<T> {
        return coroutineScope.async (start = CoroutineStart.LAZY) {
            try {
                return@async perform()
            } catch (t: Throwable) {
                logFailure(t)
                throw t
            }
        }
    }
}
