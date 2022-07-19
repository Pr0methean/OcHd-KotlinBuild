package io.github.pr0methean.ochd

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.lang.ref.SoftReference

class SoftAsyncLazy<T>(
    initialValue: T? = null,
    val supplier: suspend () -> T
) {
    private val mutex = Mutex()
    @Volatile
    private var currentValue: SoftReference<T?> = SoftReference<T?>(initialValue)
    suspend fun get(): T = currentValue.get() ?: mutex.withLock {
        currentValue.get() ?: supplier().also { currentValue = SoftReference(it) }
    }
}