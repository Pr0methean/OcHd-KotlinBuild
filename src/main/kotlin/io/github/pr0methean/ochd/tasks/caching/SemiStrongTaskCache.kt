package io.github.pr0methean.ochd.tasks.caching

import com.github.benmanes.caffeine.cache.Cache

class SemiStrongTaskCache<T>(private val backingCache: Cache<SemiStrongTaskCache<*>, Result<*>>): WeakTaskCache<T>() {
    @Suppress("UNCHECKED_CAST")
    override fun getNow(): Result<T>? {
        return backingCache.getIfPresent(this) as Result<T>? ?: super.getNow()
    }

    override fun disable() {
        enabled = false
        backingCache.invalidate(this)
    }

    override fun enabledSet(value: Result<T>?) {
        if (value == null) {
            backingCache.invalidate(this)
        } else {
            backingCache.put(this, value)
        }
        super.enabledSet(value)
    }
}