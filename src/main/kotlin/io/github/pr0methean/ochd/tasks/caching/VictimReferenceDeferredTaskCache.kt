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

    override fun disable(): Boolean = primaryCache.disable()

    override fun clear() {
        primaryCache.clear()
        coroutineRef.getAndSet(nullReference).clear()
    }

    @Suppress("DeferredIsResult", "LABEL_NAME_CLASH")
    override fun computeIfAbsent(coroutineCreator: () -> Deferred<T>): Deferred<T> {
        val nowAsync = coroutineRef.get().get()
        if (nowAsync != null) {
            return nowAsync
        }
        return primaryCache.computeIfAbsent { coroutineCreator().also { coroutineRef.set(referenceCreator(it)) } }
    }
}
