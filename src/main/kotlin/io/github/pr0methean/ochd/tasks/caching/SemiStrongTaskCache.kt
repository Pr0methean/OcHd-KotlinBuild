package io.github.pr0methean.ochd.tasks.caching

import com.github.benmanes.caffeine.cache.Cache

class SemiStrongTaskCache<T>(private val victimCache: AbstractTaskCache<T>, private val primaryCache: Cache<SemiStrongTaskCache<T>, T>):
        AbstractTaskCache<T>(victimCache.name) {
    @Suppress("UNCHECKED_CAST")
    override fun getNow(): T? {
        return primaryCache.getIfPresent(this) ?: victimCache.getNow()
    }

    override fun clear() {
        primaryCache.invalidate(this)
        victimCache.clear()
    }

    override fun disable() {
        primaryCache.invalidate(this)
        victimCache.disable()
    }

    override fun enabledSet(value: T) {
        victimCache.enabledSet(value)
        primaryCache.put(this, value)
    }
}