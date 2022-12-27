package io.github.pr0methean.ochd.tasks.caching

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.yield
import java.lang.ref.SoftReference
import java.util.concurrent.atomic.AtomicReference

private val nullReference = SoftReference<Nothing?>(null)

/**
 * A TaskCache that's backed by a soft reference.
 */
class SoftTaskCache<T>(
    name: String
): DeferredTaskCache<T>(name) {
    val coroutineRef: AtomicReference<SoftReference<out Deferred<T>?>> = AtomicReference(nullReference)
    override fun getNowAsync(): Deferred<T>? = coroutineRef.get().get()

    override fun clear() {
        coroutineRef.getAndSet(nullReference).clear()
    }

    @Suppress("DeferredIsResult", "OVERRIDE_BY_INLINE")
    override suspend inline fun computeIfAbsent(coroutineCreator: () -> Deferred<T>): Deferred<T> {
        while (true) {
            val currentCoroutineRef = coroutineRef.get()
            val currentCoroutine = currentCoroutineRef.get()
            if (currentCoroutine != null) {
                return currentCoroutine
            }
            val newCoroutine = coroutineCreator()
            if (!isEnabled() || coroutineRef.compareAndSet(currentCoroutineRef, SoftReference(newCoroutine))) {
                return newCoroutine
            }
            yield() // Spin wait
        }
    }
}
