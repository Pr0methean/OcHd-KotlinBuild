package io.github.pr0methean.ochd.tasks.caching

import java.lang.ref.WeakReference

@Suppress("unused")
open class WeakTaskCache<T>(name: String): AbstractTaskCache<T>(name) {
    @Volatile var result: WeakReference<out Result<T>?> = nullRef
    override fun disable() {}

    override fun clear() {
        result.clear()
    }

    override fun getNow(): Result<T>? = result.get()

    override fun enabledSet(value: Result<T>) {
        result = WeakReference(value)
    }
}