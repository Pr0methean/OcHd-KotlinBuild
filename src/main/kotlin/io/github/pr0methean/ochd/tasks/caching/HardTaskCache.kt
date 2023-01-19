package io.github.pr0methean.ochd.tasks.caching

import kotlinx.coroutines.Deferred
import java.util.concurrent.atomic.AtomicReference

@Suppress("unused")
class HardTaskCache<T>(
    name: String
): DeferredTaskCache<T>(name) {
    val coroutineRef: AtomicReference<Deferred<T>?> = AtomicReference(null)

    override fun getNowAsync(): Deferred<T>? = coroutineRef.get()

    override fun clear() {
        coroutineRef.set(null)
    }

    @Suppress("OVERRIDE_BY_INLINE")
    override inline fun computeIfAbsent(coroutineCreator: () -> Deferred<T>): Deferred<T> {
        return coroutineRef.compareAndExchange(null, coroutineCreator())!!
    }
}
