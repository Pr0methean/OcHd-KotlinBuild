package io.github.pr0methean.ochd.tasks.caching

import com.github.benmanes.caffeine.cache.Cache
import java.lang.ref.Cleaner
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean

private val CLEANER = Cleaner.create { runnable ->
    val thread = Thread.ofVirtual().unstarted(runnable)
    thread.priority = Thread.MAX_PRIORITY
    thread
}
class SemiStrongTaskCache<T>(private val baseCache: AbstractTaskCache<T>, private val backingCache: Cache<SemiStrongTaskCache<*>, Result<*>>):
        AbstractTaskCache<T>(baseCache.name) {
    private val cleanupScheduled = AtomicBoolean(false)
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
        val backingCacheRef = WeakReference(backingCache)
        val cleanUpCacheRunnable = Runnable {
            backingCacheRef.get()?.cleanUp()
        }
        if (cleanupScheduled.compareAndSet(false, true)) {
            CLEANER.register(this, cleanUpCacheRunnable)
        }
        if (value.isSuccess && (baseCache is WeakTaskCache || baseCache is SoftTaskCache)) {
            CLEANER.register(value, cleanUpCacheRunnable)
        }
        baseCache.enabledSet(value)
        backingCache.put(this, value)
    }
}