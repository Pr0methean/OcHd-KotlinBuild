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
    override inline fun computeIfAbsent(crossinline coroutineCreator: () -> Deferred<T>): Deferred<T> {
        var currentCoroutineRef: SoftReference<out Deferred<T>?> = coroutineRef.get()
        val newCoroutine by lazy { coroutineCreator() }
        val newCoroutineRef by lazy { SoftReference(newCoroutine) }
        while (true) {
            val currentCoroutine = currentCoroutineRef.get()
            if (currentCoroutine != null) {
                return currentCoroutine
            }
            if (!isEnabled()) {
                return newCoroutine
            }
            val oldRef = currentCoroutineRef
            currentCoroutineRef = coroutineRef.compareAndExchange(oldRef, newCoroutineRef)
            if (currentCoroutineRef === oldRef) {
                return newCoroutine
            }
            Thread.onSpinWait()
        }
    }
}
