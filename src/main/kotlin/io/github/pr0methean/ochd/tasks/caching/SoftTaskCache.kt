package io.github.pr0methean.ochd.tasks.caching

import kotlinx.coroutines.Deferred
import java.lang.ref.Reference
import java.lang.ref.SoftReference
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicReference

val nullReference: WeakReference<Nothing?> = WeakReference<Nothing?>(null)

/**
 * A [DeferredTaskCache] that's backed by a soft reference.
 */
@Suppress("unused")
class SoftTaskCache<T>(
    name: String
): DeferredTaskCache<T>(name) {
    val coroutineRef: AtomicReference<Reference<out Deferred<T>?>> = AtomicReference(nullReference)
    override fun getNowAsync(): Deferred<T>? = coroutineRef.get().get()

    override fun clear() {
        coroutineRef.updateAndGet {
            if (it is WeakReference) {
                it
            } else {
                val current = it.get()
                if (current == null) {
                    nullReference
                } else {
                    WeakReference(current)
                }
            }
        }
    }

    @Suppress("DeferredIsResult", "OVERRIDE_BY_INLINE")
    override inline fun computeIfAbsent(crossinline coroutineCreator: () -> Deferred<T>): Deferred<T> {
        var currentCoroutineRef: Reference<out Deferred<T>?> = coroutineRef.get()
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
