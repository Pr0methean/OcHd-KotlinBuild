package io.github.pr0methean.ochd.tasks.caching

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.yield
import java.lang.ref.Reference
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicReference

private val nullReference = WeakReference<Nothing?>(null)

/**
 * A TaskCache that's backed by a soft or weak reference.
 */
class ReferenceTaskCache<T>(
    val referenceCreator: (Deferred<T>) -> Reference<Deferred<T>>,
    name: String
): DeferredTaskCache<T>(name) {
    val coroutineRef: AtomicReference<Reference<out Deferred<T>?>> = AtomicReference(nullReference)
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
            if (!isEnabled()) {
                return newCoroutine
            }
            if (coroutineRef.compareAndSet(currentCoroutineRef, referenceCreator(newCoroutine))) {
                return newCoroutine
            }
            yield() // Spin wait
        }
    }
}
