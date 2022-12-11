package io.github.pr0methean.ochd.tasks.caching

import kotlinx.coroutines.Deferred

object NoopDeferredTaskCache : DeferredTaskCache<Any?>() {
    override fun getNowAsync(): Deferred<*>? = null

    override fun clear() {
        /* No-op. */
    }

    override fun enable(): Boolean = false

    @Suppress("DeferredIsResult")
    override fun computeIfAbsent(coroutineCreator: () -> Deferred<Any?>): Deferred<Any?> = coroutineCreator()
}

@Suppress("UNCHECKED_CAST")
fun <T> noopDeferredTaskCache(): DeferredTaskCache<T> = NoopDeferredTaskCache as DeferredTaskCache<T>
