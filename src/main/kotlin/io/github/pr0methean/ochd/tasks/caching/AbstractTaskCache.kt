package io.github.pr0methean.ochd.tasks.caching

abstract class AbstractTaskCache<T> : TaskCache<T> {
    @Volatile var enabled = false
    override fun disable() {
        enabledSet(null)
        enabled = false
    }

    override fun enable() {
        enabled = true
    }

    override fun set(value: Result<T>?) {
        if (enabled) {
            enabledSet(value)
        }
    }

    abstract fun enabledSet(value: Result<T>?)
}