package io.github.pr0methean.ochd.tasks.caching

import kotlinx.coroutines.Deferred

/**
 * If a task uses this, results will not be stored. Every request for the result of the task will trigger a new
 * computation.
 */
object NoopDeferredTaskCache : DeferredTaskCache<Any?>() {
    override fun getNowAsync(): Deferred<*>? = null

    override fun clear() {
        /* No-op. */
    }

    override fun enable(): Boolean = false

    @Suppress("DeferredIsResult", "OVERRIDE_BY_INLINE")
    override suspend inline fun computeIfAbsent(coroutineCreator: () -> Deferred<Any?>): Deferred<Any?>
            = coroutineCreator()
}

/**
 * Returns a [DeferredTaskCache] that doesn't store the coroutine. Every request for the result of the task will trigger
 * a new computation.
 */
@Suppress("UNCHECKED_CAST")
fun <T> noopDeferredTaskCache(): DeferredTaskCache<T> = NoopDeferredTaskCache as DeferredTaskCache<T>
