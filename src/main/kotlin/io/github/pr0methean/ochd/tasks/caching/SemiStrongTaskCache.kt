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
class SemiStrongTaskCache<T>(private val victimCache: AbstractTaskCache<T>, private val primaryCache: Cache<SemiStrongTaskCache<*>, Result<*>>):
        AbstractTaskCache<T>(victimCache.name) {
    private val cleanupScheduled = AtomicBoolean(false)
    @Suppress("UNCHECKED_CAST")
    override fun getNow(): Result<T>? {
        return primaryCache.getIfPresent(this) as Result<T>? ?: victimCache.getNow()
    }

    override fun clear() {
        primaryCache.invalidate(this)
        victimCache.clear()
    }

    override fun disable() {
        primaryCache.invalidate(this)
        victimCache.disable()
    }

    override fun enabledSet(value: Result<T>) {
        val backingCacheRef = WeakReference(primaryCache)
        val cleanUpCacheRunnable = Runnable {
            backingCacheRef.get()?.cleanUp()
        }
        if (cleanupScheduled.compareAndSet(false, true)) {
            CLEANER.register(this, cleanUpCacheRunnable)
        }
        if (victimCache is WeakTaskCache || victimCache is SoftTaskCache) {
            CLEANER.register(value, cleanUpCacheRunnable)
        }
        victimCache.enabledSet(value)
        primaryCache.put(this, value)
    }
}