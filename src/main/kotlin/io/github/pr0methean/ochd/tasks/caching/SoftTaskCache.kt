package io.github.pr0methean.ochd.tasks.caching

import java.lang.ref.SoftReference

@Suppress("unused")
open class SoftTaskCache<T>(name: String): AbstractTaskCache<T>(name) {
    @Volatile var result: SoftReference<Result<T>?> = SoftReference<Result<T>?>(null)
    override fun getNow(): Result<T>? = result.get()

    override fun enabledSet(value: Result<T>?) {
        result = SoftReference(value)
    }
}