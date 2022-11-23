package io.github.pr0methean.ochd.tasks.caching

import java.lang.ref.Reference
import java.lang.ref.SoftReference
import java.lang.ref.WeakReference

@Suppress("unused")
open class SoftTaskCache<T>(name: String): AbstractTaskCache<T>(name) {
    @Volatile var result: Reference<out T?> = nullRef
    override fun enabledSet(value: T) {
        result = SoftReference(value)
    }

    override fun disable() {
        result = WeakReference(getNow())
    }

    override fun clear() {
        result.clear()
    }

    override fun getNow(): T? = result.get()
}