package io.github.pr0methean.ochd.tasks.caching

import com.github.benmanes.caffeine.cache.Cache

/**
 * A TaskCache that's backed by a Caffeine cache (the primary cache) and another TaskCache instance (the victim cache).
 * So named because of the primary cache is limited in size and the victim cache is a [SoftTaskCache] or [WeakTaskCache]
 * then the total number of cached values will have a minimum defined in code but a maximum that depends on heap size,
 * yielding a cache hit rate that scales almost optimally with the available heap space (i.e. the amount of space not
 * occupied by strongly-reachable objects).
 */
class SemiStrongTaskCache<T>(
        private val victimCache: TaskCache<T>,
        private val primaryCache: Cache<SemiStrongTaskCache<T>, T>
): AbstractTaskCache<T>(victimCache.name) {
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
