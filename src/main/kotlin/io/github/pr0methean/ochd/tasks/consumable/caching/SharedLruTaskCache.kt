package io.github.pr0methean.ochd.tasks.consumable.caching

import com.github.benmanes.caffeine.cache.Caffeine
import java.lang.ref.Reference

class SharedLruTaskCache(capacity: Long) {
    val lruCache = Caffeine.newBuilder().weakKeys().softValues().maximumSize(capacity).build<Any, Result<*>>()
    fun <T> newKey(): TaskCache<T> {
        val key = Object()
        return object : TaskCache<T> {
            @Suppress("UNCHECKED_CAST")
            override fun getNow(): Result<T>? {
                val result = lruCache.getIfPresent(key) as Result<T>?
                Reference.reachabilityFence(key)
                return result
            }

            override fun set(value: Result<T>?) {
                if (value != null) {
                    lruCache.put(key, value)
                } else {
                    lruCache.invalidate(key)
                }
                Reference.reachabilityFence(key)
            }

        }
    }
}