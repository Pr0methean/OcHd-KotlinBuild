package io.github.pr0methean.ochd

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

abstract class AsyncLazy<T> {
    val mutex = Mutex()

    suspend fun get(): T = getNow() ?: mutex.withLock {
        getNow() ?: getFromSupplierAndStore()
    }

    protected abstract suspend fun getFromSupplierAndStore(): T
    abstract fun getNow(): T?

    protected abstract fun set(value: T?)

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
}