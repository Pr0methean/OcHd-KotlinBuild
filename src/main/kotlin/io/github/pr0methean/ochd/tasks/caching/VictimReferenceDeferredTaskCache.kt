package io.github.pr0methean.ochd.tasks.caching

import kotlinx.coroutines.Deferred
import java.lang.ref.Reference
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicReference

private val nullReference = WeakReference<Nothing?>(null)

/**
 * A TaskCache that's backed by a Caffeine cache (the primary cache) and a soft or weak reference (the victim cache).
 * So named because the cached coroutine will tend to last longer than if we only used the soft reference, but not as
 * long as if the Caffeine cache was unlimited.
 */
class VictimReferenceDeferredTaskCache<T>(
    private val primaryCache: DeferredTaskCache<T>,
    val referenceCreator: (Deferred<T>) -> Reference<Deferred<T>>
): DeferredTaskCache<T>() {
    private val coroutineRef: AtomicReference<Reference<out Deferred<T>?>> = AtomicReference(nullReference)
    override fun getNowAsync(): Deferred<T>? = primaryCache.getNowAsync() ?: coroutineRef.get().get()
    override fun enable(): Boolean {
        val enabledSuper = super.enable()
        val enabledPrimary = primaryCache.enable()
        return enabledPrimary || enabledSuper
    }

    override fun disable(): Boolean {
        val disabledPrimary = primaryCache.disable()
        val disabledSuper = super.disable()
        return disabledPrimary || disabledSuper
    }

    override fun clear() {
        primaryCache.clear()
        coroutineRef.getAndSet(nullReference).clear()
    }

    @Suppress("DeferredIsResult")
    override suspend fun computeIfAbsent(coroutineCreator: () -> Deferred<T>): Deferred<T> {
        while (true) {
            val currentCoroutineRef = coroutineRef.get()
            val currentCoroutine = currentCoroutineRef.get()
            if (currentCoroutine != null) {
                return currentCoroutine
            }
            val newCoroutine = coroutineCreator()
            if (coroutineRef.compareAndSet(currentCoroutineRef, referenceCreator(newCoroutine))) {
                return if (isEnabled()) primaryCache.computeIfAbsent {newCoroutine} else newCoroutine
            }
        }
    }
}
