package io.github.pr0methean.ochd.tasks.caching

import kotlinx.coroutines.Deferred
import java.util.concurrent.atomic.AtomicReference

class HardTaskCache<T>(name: String): DeferredTaskCache<T>(name) {
    private val coroutineRef = AtomicReference<Deferred<T>?>(null)
    override fun getNowAsync(): Deferred<T>? = coroutineRef.get()

    override fun clear() {
        coroutineRef.set(null)
    }

    @Suppress("DeferredIsResult")
    override suspend fun computeIfAbsent(coroutineCreator: () -> Deferred<T>): Deferred<T> {
        if (!isEnabled()) {
            return coroutineCreator()
        }
        val newCoroutine = coroutineCreator()
        val oldCoroutine = coroutineRef.compareAndExchange(null, newCoroutine)
        return oldCoroutine ?: newCoroutine
    }
}