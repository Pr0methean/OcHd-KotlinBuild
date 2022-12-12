package io.github.pr0methean.ochd.tasks.caching

import kotlinx.coroutines.Deferred
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Stores the coroutine running a particular [io.github.pr0methean.ochd.tasks.Task] so multiple consuming jobs can await
 * the same coroutine and/or reuse its result. May be backed by an evictable entry in a Caffeine cache
 * and/or a soft or weak reference, to support memory-constrained reuse. A separate
 * instance is needed for each Task (unless it uses [NoopDeferredTaskCache]), and that Task should be the only object that
 * references it strongly.
 */
abstract class DeferredTaskCache<out T> {
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

    @Suppress("DeferredIsResult")
    abstract suspend fun computeIfAbsent(coroutineCreator: () -> Deferred<@UnsafeVariance T>): Deferred<T>
}
