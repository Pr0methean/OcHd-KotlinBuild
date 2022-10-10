package io.github.pr0methean.ochd.tasks.caching

import com.github.benmanes.caffeine.cache.Cache

class SemiStrongTaskCache<T>(private val baseCache: AbstractTaskCache<T>, private val backingCache: Cache<SemiStrongTaskCache<*>, Result<*>>):
        AbstractTaskCache<T>(baseCache.name) {
    @Suppress("UNCHECKED_CAST")
    override fun getNow(): Result<T>? {
        return backingCache.getIfPresent(this) as Result<T>? ?: baseCache.getNow()
    }

    override fun clear() {
        baseCache.clear()
        backingCache.invalidate(this)
    }

    override fun disable() {
        baseCache.disable()
        backingCache.invalidate(this)
    }

    override fun enabledSet(value: Result<T>) {
        baseCache.enabledSet(value)
        backingCache.put(this, value)
    }
}