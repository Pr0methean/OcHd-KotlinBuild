package io.github.pr0methean.ochd.tasks.caching

import java.lang.ref.WeakReference

@Suppress("unused")
open class WeakTaskCache<T>(name: String): AbstractTaskCache<T>(name) {
    @Volatile var result: WeakReference<out T?> = nullRef

    override fun clear() {
        super.clear()
        result.clear()
    }

    override fun getNow(): T? = result.get()

    override fun enabledSet(value: T) {
        result = WeakReference(value)
    }
}