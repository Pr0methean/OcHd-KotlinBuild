package io.github.pr0methean.ochd.tasks.caching

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.util.concurrent.atomic.AtomicBoolean

abstract class DeferredTaskCache<out T> {
    private val enabled = AtomicBoolean(false)
    abstract fun getNowAsync(): Deferred<T>?

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getNow(): Result<T>? {
        val nowAsync = getNowAsync() ?: return null
        return if (nowAsync.isCompleted) {
            nowAsync.runCatching(Deferred<T>::getCompleted)
        } else null
    }

    abstract fun clear()

    open fun enable(): Boolean = !enabled.getAndSet(true)

    fun disable(): Boolean {
        if (enabled.getAndSet(false)) {
            clear()
            return true
        }
        return false
    }

    fun isEnabled(): Boolean = enabled.get()

    fun getAsync(coroutineCreator: () -> Deferred<@UnsafeVariance T>): Deferred<T> = if (enabled.get()) {
        computeIfAbsent(coroutineCreator)
    } else {
        coroutineCreator()
    }

    @Suppress("DeferredIsResult")
    abstract fun computeIfAbsent(coroutineCreator: () -> Deferred<@UnsafeVariance T>): Deferred<T>
}
