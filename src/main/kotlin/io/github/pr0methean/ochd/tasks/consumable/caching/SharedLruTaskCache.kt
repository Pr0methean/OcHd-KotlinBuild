package io.github.pr0methean.ochd.tasks.consumable.caching

import com.github.benmanes.caffeine.cache.Caffeine
import java.lang.ref.Reference

class SharedLruTaskCache(capacity: Long) {
    val lruCache = Caffeine.newBuilder().weakKeys().softValues().maximumSize(capacity).build<TaskCache<*>, Result<*>>()
    fun <T> newKey(): TaskCache<T> {
        return object : TaskCache<T> {
            @Suppress("UNCHECKED_CAST")
            override fun getNow(): Result<T>? {
                val result = lruCache.getIfPresent(this) as Result<T>?
                Reference.reachabilityFence(this)
                return result
            }

            override fun set(value: Result<T>?) {
                if (value != null) {
                    lruCache.put(this, value)
                } else {
                    lruCache.invalidate(this)
                }
                Reference.reachabilityFence(this)
            }

        }
    }
}