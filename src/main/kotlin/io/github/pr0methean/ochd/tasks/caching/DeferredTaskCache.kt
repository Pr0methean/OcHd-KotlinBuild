package io.github.pr0methean.ochd.tasks.caching

import kotlinx.coroutines.Deferred
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Stores the coroutine running a particular [io.github.pr0methean.ochd.tasks.AbstractTask] so multiple consuming jobs
 * can await the same coroutine or reuse an already-computed result. May be backed by an evictable entry in a Caffeine
 * cache and/or a soft or weak reference, to support memory-constrained reuse. A separate
 * instance is needed for each Task (unless it uses [NoopDeferredTaskCache]), and that Task (and the DeferredTaskCache's
 * own members) should be the only objects that reference the coroutine once it's completed.
 */
abstract class DeferredTaskCache<T>(val name: String) {
    override fun toString(): String = name

    private val enabled = AtomicBoolean(false)
    abstract fun getNowAsync(): Deferred<T>?

    abstract fun clear()

    open fun enable(): Boolean = !enabled.getAndSet(true)

    open fun disable(): Boolean {
        if (enabled.getAndSet(false)) {
            clear()
            return true
        }
        return false
    }

    fun isEnabled(): Boolean = enabled.get()

    /**
     * If this cache is enabled, atomically creates the coroutine if it is absent by calling [coroutineCreator]().
     * If it is disabled and has no coroutine, returns [coroutineCreator]() without storing it in cache. If it is
     * disabled but still has a coroutine, returns that coroutine.
     */
    @Suppress("DeferredIsResult")
    abstract fun computeIfAbsent(coroutineCreator: () -> Deferred<T>): Deferred<T>
}
