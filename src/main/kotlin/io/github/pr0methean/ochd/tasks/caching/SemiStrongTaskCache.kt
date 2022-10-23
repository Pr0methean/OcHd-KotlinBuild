package io.github.pr0methean.ochd.tasks.caching

import com.github.benmanes.caffeine.cache.Cache
import java.lang.ref.Cleaner

private val CLEANER = Cleaner.create()
class SemiStrongTaskCache<T>(private val baseCache: AbstractTaskCache<T>, private val backingCache: Cache<SemiStrongTaskCache<*>, Result<*>>):
        AbstractTaskCache<T>(baseCache.name) {
    @Suppress("UNCHECKED_CAST")
    override fun getNow(): Result<T>? {
        return backingCache.getIfPresent(this) as Result<T>? ?: baseCache.getNow()
    }

    override fun clear() {
        backingCache.invalidate(this)
        baseCache.clear()
    }

    override fun disable() {
        backingCache.invalidate(this)
        baseCache.disable()
    }

    override fun enabledSet(value: Result<T>) {
        CLEANER.register(this, backingCache::cleanUp)
        if (baseCache is WeakTaskCache || baseCache is SoftTaskCache) {
            CLEANER.register(value, backingCache::cleanUp)
        }
        baseCache.enabledSet(value)
        backingCache.put(this, value)
    }
}