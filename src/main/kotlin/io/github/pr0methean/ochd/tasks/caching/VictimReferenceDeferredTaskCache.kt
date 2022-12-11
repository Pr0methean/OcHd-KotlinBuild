package io.github.pr0methean.ochd.tasks.caching

import kotlinx.coroutines.Deferred
import java.lang.ref.Reference
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicReference

private val nullReference = WeakReference<Nothing?>(null)

class VictimReferenceDeferredTaskCache<T>(
    private val primaryCache: DeferredTaskCache<T>,
    val referenceCreator: (Deferred<T>) -> Reference<Deferred<T>>
): DeferredTaskCache<T>() {
    private val coroutineRef: AtomicReference<Reference<out Deferred<T>?>> = AtomicReference(nullReference)
    override fun getNowAsync(): Deferred<T>? = primaryCache.getNowAsync() ?: coroutineRef.get().get()

    override fun clear() {
        primaryCache.clear()
        coroutineRef.getAndSet(nullReference).clear()
    }

    @Suppress("DeferredIsResult", "LABEL_NAME_CLASH")
    override fun computeIfAbsent(coroutineCreator: () -> Deferred<T>): Deferred<T> = primaryCache.computeIfAbsent {
        val coroutine = coroutineCreator()
        return@computeIfAbsent coroutineRef.updateAndGet {
            if (it.get() != null) {
                it
            } else {
                referenceCreator(coroutine)
            }
        }.get()!!
    }
}
