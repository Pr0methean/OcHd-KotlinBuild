package io.github.pr0methean.ochd

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.annotation.concurrent.GuardedBy

abstract class AsyncLazy<T> {
    val mutex = Mutex()
    @Volatile private var started: Boolean = false

    suspend fun get(): T {
        return getNow() ?: withContext(currentCoroutineContext().plus(SupervisorJob())) {
            try {
                return@withContext mutex.withLock {
                    getNow() ?: let {
                        started = true
                        return@withLock getFromSupplierAndStore()
                    }
                }
            } catch (e: CancellationException) {
                // Don't cancel siblings
                throw RuntimeException(e)
            }
        }
    }

    protected abstract suspend fun getFromSupplierAndStore(): T
    abstract fun getNow(): T?

    @GuardedBy("mutex")
    abstract fun set(value: T?)

    suspend fun clearResult() {
        mutex.withLock {
            started = false
            set(null)
        }
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