package io.github.pr0methean.ochd.tasks.caching

import com.github.benmanes.caffeine.cache.Cache
import javafx.scene.image.Image

const val MAX_IMAGE_PIXELS_TO_CACHE_NONWEAKLY = 1L.shl(24)
class SemiStrongTaskCache<T>(name: String, private val backingCache: Cache<SemiStrongTaskCache<*>, Result<*>>):
        WeakTaskCache<T>(name) {
    override var enabled: Boolean
        get() = super.enabled
        set(value) {
            super.enabled = value
            if (!value) {
                backingCache.invalidate(this)
            }
        }

    @Suppress("UNCHECKED_CAST")
    override fun getNow(): Result<T>? {
        return backingCache.getIfPresent(this) as Result<T>? ?: super.getNow()
    }

    override fun enabledSet(value: Result<T>?) {
        if (value == null) {
            backingCache.invalidate(this)
        } else {
            super.enabledSet(value)
            if (value.isSuccess) {
                val successValue = value.getOrThrow()
                if (successValue is Image
                    && (successValue.height * successValue.width.toLong()) > MAX_IMAGE_PIXELS_TO_CACHE_NONWEAKLY
                ) {
                    super.enabled = false
                    return
                }
            }
            backingCache.put(this, value)
        }
    }
}