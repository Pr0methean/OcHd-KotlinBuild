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
    override fun enable(): Boolean {
        val enabledSuper = super.enable()
        val enabledPrimary = primaryCache.enable()
        return enabledPrimary || enabledSuper
    }

    override fun disable(): Boolean {
        val disabledPrimary = primaryCache.disable()
        val disabledSuper = enabled.getAndSet(false)
        if (disabledSuper) {
            coroutineRef.updateAndGet { WeakReference(it.get()) }
        }
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
