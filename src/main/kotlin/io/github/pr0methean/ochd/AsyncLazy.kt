package io.github.pr0methean.ochd

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

abstract class AsyncLazy<T> {
    val mutex = Mutex()

    suspend fun get(): T = getNow() ?: mutex.withLock {
        getNow() ?: getFromSupplier().also(::set)
    }

    protected abstract suspend fun getFromSupplier(): T
    abstract fun getNow(): T?

    protected abstract fun set(value: T?)

    suspend fun mergeWithDuplicate(other: AsyncLazy<T>) {
        if (getNow() == null) {
            mutex.withLock {
                if (getNow() == null) {
                    set(other.getNow())
                }
            }
        }
    }

    suspend fun setIfEmpty(value: T) {
        if (getNow() == null) {
            mutex.withLock {
                if (getNow() == null) {
                    set(value)
                }
            }
        }
    }

    fun start(scope: CoroutineScope) {
        scope.launch { get() }
    }
}