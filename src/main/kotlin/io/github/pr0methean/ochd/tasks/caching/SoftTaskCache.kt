package io.github.pr0methean.ochd.tasks.caching

import java.lang.ref.Reference
import java.lang.ref.SoftReference
import java.lang.ref.WeakReference

@Suppress("unused")
open class SoftTaskCache<T>(name: String): AbstractTaskCache<T>(name) {
    @Volatile var result: Reference<Result<T>?> = WeakReference<Result<T>?>(null)
    override fun enabledSet(value: Result<T>) {
        result = SoftReference(value)
    }

    override fun disable() {
        result = WeakReference(getNow())
    }

    override fun clear() {
        result = WeakReference(null)
    }

    override fun getNow(): Result<T>? = result.get()
}