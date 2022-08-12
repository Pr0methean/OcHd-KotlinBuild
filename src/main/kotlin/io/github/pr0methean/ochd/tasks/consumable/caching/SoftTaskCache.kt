package io.github.pr0methean.ochd.tasks.consumable.caching

import java.lang.ref.SoftReference


class SoftTaskCache<T>: AbstractTaskCache<T>() {
    @Volatile var result = SoftReference<Result<T>>(null)
    override fun getNow(): Result<T>? = result.get()

    override fun enabledSet(value: Result<T>?) {
        result = SoftReference(value)
    }
}