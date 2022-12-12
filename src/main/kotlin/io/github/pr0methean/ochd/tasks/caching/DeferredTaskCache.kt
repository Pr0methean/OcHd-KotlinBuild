package io.github.pr0methean.ochd.tasks.caching

import kotlinx.coroutines.Deferred
import java.util.concurrent.atomic.AtomicBoolean

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
