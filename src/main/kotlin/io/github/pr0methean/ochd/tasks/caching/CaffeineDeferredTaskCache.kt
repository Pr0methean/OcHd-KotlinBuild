package io.github.pr0methean.ochd.tasks.caching

import com.github.benmanes.caffeine.cache.Cache
import kotlinx.coroutines.Deferred

class CaffeineDeferredTaskCache<T>(private val caffeineCache: Cache<in CaffeineDeferredTaskCache<T>, Deferred<T>>)
    : DeferredTaskCache<T>() {
    override fun getNowAsync(): Deferred<T>? = caffeineCache.getIfPresent(this)

    override fun clear(): Unit = caffeineCache.invalidate(this)

    @Suppress("DeferredIsResult")
    override fun computeIfAbsent(coroutineCreator: () -> Deferred<T>): Deferred<T>
            = caffeineCache.get(this) {coroutineCreator()}
}
