package io.github.pr0methean.ochd.tasks.caching

import com.github.benmanes.caffeine.cache.Cache
import kotlinx.coroutines.Deferred

/**
 * DeferredTaskCache backed by a Caffeine cache.
 */
class CaffeineDeferredTaskCache<T>(val caffeineCache: Cache<in CaffeineDeferredTaskCache<T>, Deferred<T>>)
    : DeferredTaskCache<T>() {
    override fun getNowAsync(): Deferred<T>? = caffeineCache.getIfPresent(this)

    override fun clear(): Unit = caffeineCache.invalidate(this)

    @Suppress("DeferredIsResult", "OVERRIDE_BY_INLINE")
    override suspend inline fun computeIfAbsent(crossinline coroutineCreator: () -> Deferred<T>): Deferred<T> {
        return if(isEnabled()) {
            caffeineCache.get(this) { coroutineCreator() }
        } else coroutineCreator()
    }
}
