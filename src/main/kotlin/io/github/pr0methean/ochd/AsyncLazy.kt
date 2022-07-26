package io.github.pr0methean.ochd

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.annotation.concurrent.GuardedBy

abstract class AsyncLazy<T> {
    val mutex = Mutex()
    @Volatile private var started: Boolean = false

    suspend fun get(): T = getNow() ?: mutex.withLock {
        getNow() ?: let {
            started = true
            getFromSupplierAndStore()
        }
    }

    protected abstract suspend fun getFromSupplierAndStore(): T
    abstract fun getNow(): T?

    @GuardedBy("mutex")
    abstract fun set(value: T?)

    suspend fun compareAndSet(expected: T?, newValue: T?): T? = mutex.withLock {
        val actual = getNow()
        if (actual == expected) {
            set(newValue)
        }
        return actual
    }

    @Suppress("unused")
    suspend fun mergeWithDuplicate(other: AsyncLazy<T>) {
        if (getNow() == null) {
            mutex.withLock {
                if (getNow() == null) {
                    set(other.getNow())
                }
            }
        }
    }

    fun start(scope: CoroutineScope) {
        scope.launch { get() }
    }

    fun isStarted(): Boolean = getNow() != null || started
}