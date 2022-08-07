package io.github.pr0methean.ochd.tasks.consumable.caching

import java.lang.ref.SoftReference


class SoftTaskCache<T>: TaskCache<T> {
    @Volatile var result = SoftReference<Result<T>>(null)
    override fun getNow(): Result<T>? = result.get()

    override fun set(value: Result<T>?) {
        result = SoftReference(value)
    }
}