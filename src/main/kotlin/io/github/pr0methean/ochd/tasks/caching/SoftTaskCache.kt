package io.github.pr0methean.ochd.tasks.caching

import kotlinx.coroutines.Deferred
import java.lang.ref.SoftReference
import java.util.concurrent.atomic.AtomicReference

val nullReference: SoftReference<Nothing?> = SoftReference<Nothing?>(null)

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
    override inline fun computeIfAbsent(coroutineCreator: () -> Deferred<T>): Deferred<T> {
        val newCoroutine = coroutineCreator()
        if (!isEnabled()) {
            return newCoroutine
        }
        var currentCoroutineRef: SoftReference<out Deferred<T>?> = nullReference
        val newCoroutineRef = SoftReference(newCoroutine)
        while (true) {
            val oldRef = currentCoroutineRef
            currentCoroutineRef = coroutineRef.compareAndExchange(oldRef, newCoroutineRef)
            if (currentCoroutineRef === oldRef || !isEnabled()) {
                return newCoroutine
            }
            val currentCoroutine = currentCoroutineRef.get()
            if (currentCoroutine != null) {
                return currentCoroutine
            }
            Thread.onSpinWait()
        }
    }
}
