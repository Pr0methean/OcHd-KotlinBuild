package io.github.pr0methean.ochd

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AsyncLazy<T>(
    initialValue: T? = null,
    val supplier: suspend () -> T
) {
    @Volatile var result = initialValue
    private val mutex = Mutex()
    suspend fun get(): T = result ?: mutex.withLock {
        result ?: supplier().also { result = it }
    }
}