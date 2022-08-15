package io.github.pr0methean.ochd.tasks.consumable.caching

import java.lang.ref.WeakReference

@Suppress("unused")
open class WeakTaskCache<T>: AbstractTaskCache<T>() {
    @Volatile var result = WeakReference<Result<T>>(null)
    override fun enabledSet(value: Result<T>?) {
        set(value)
    }

    override fun getNow(): Result<T>? = result.get()
    override fun set(value: Result<T>?) {
        result = WeakReference(value)
    }
}