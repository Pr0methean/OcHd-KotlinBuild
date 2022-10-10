package io.github.pr0methean.ochd.tasks.caching

import com.github.benmanes.caffeine.cache.Cache

class SemiStrongTaskCache<T>(private val baseCache: AbstractTaskCache<T>,
                             private val caffeineCache: Cache<SemiStrongTaskCache<*>, Result<*>>):
        AbstractTaskCache<T>(baseCache.name) {
    override var enabled: Boolean
        get() = super.enabled
        set(value) {
            super.enabled = value
            if (!value) {
                caffeineCache.invalidate(this)
            }
        }

    @Suppress("UNCHECKED_CAST")
    override fun getNow(): Result<T>? {
        return caffeineCache.getIfPresent(this) as Result<T>? ?: baseCache.getNow()
    }

    override fun enabledSet(value: Result<T>?) {
        if (value == null) {
            baseCache.enabledSet(null)
            caffeineCache.invalidate(this)
        } else {
            baseCache.enabledSet(value)
            caffeineCache.put(this, value)
        }
    }
}