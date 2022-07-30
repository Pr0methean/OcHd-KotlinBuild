package io.github.pr0methean.ochd.tasks.consumable.caching

import java.lang.ref.WeakReference

class WeakTaskCache<T>: TaskCache<T> {
    @Volatile var result = WeakReference<Result<T>>(null)
    override fun getNow(): Result<T>? = result.get()

    override fun set(value: Result<T>?) {
        result = WeakReference(value)
    }
}