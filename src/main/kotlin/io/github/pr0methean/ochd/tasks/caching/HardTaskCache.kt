package io.github.pr0methean.ochd.tasks.caching

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import java.util.concurrent.atomic.AtomicReference

/**
 * A [DeferredTaskCache] that's backed by a strong reference. Since the cached image won't be evicted while it's still
 * needed, this is the preferred implementation for reusable tasks as long as sufficient memory is available.
 */
@Suppress("unused")
class HardTaskCache<T>(
    name: String
): DeferredTaskCache<T>(name) {
    val coroutineRef: AtomicReference<Deferred<T>?> = AtomicReference(null)

    override fun getNowAsync(): Deferred<T>? = coroutineRef.get()

    override fun clear() {
        coroutineRef.set(null)
    }

    override fun setValue(newValue: T) {
        if (isEnabled()) {
            coroutineRef.set(CompletableDeferred(newValue))
        }
    }

    @Suppress("OVERRIDE_BY_INLINE")
    override inline fun computeIfAbsent(coroutineCreator: () -> Deferred<T>): Deferred<T> {
        val newCoroutine = coroutineCreator()
        return if (isEnabled()) {
            coroutineRef.compareAndExchange(null, newCoroutine) ?: newCoroutine
        } else newCoroutine
    }
}
