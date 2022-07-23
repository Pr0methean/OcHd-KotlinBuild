package io.github.pr0methean.ochd

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AsyncLazy<T>(
    val supplier: suspend () -> T
) {
    @Volatile var result: T? = null
    private val mutex = Mutex()
    suspend fun get(): T = result ?: mutex.withLock {
        result ?: supplier().also { result = it }
    }
}