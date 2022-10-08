package io.github.pr0methean.ochd.tasks.caching

import java.lang.ref.Reference
import java.lang.ref.SoftReference
import java.lang.ref.WeakReference

@Suppress("unused")
open class SoftTaskCache<T>(name: String): AbstractTaskCache<T>(name) {
    @Volatile var result: Reference<Result<T>?> = SoftReference<Result<T>?>(null)
    override fun getNow(): Result<T>? = result.get()

    override fun enabledSet(value: Result<T>?) {
        result = if (value == null) {
            WeakReference(getNow())
        } else {
            SoftReference(value)
        }
    }
}