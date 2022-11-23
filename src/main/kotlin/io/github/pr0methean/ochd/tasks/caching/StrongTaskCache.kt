package io.github.pr0methean.ochd.tasks.caching

@Suppress("unused")
class StrongTaskCache<T>(name: String): AbstractTaskCache<T>(name) {
    @Volatile var result: T? = null
    override fun disable() {
        result = null
    }

    override fun clear() {
        result = null
    }

    override fun enabledSet(value: T) {
        result = value
    }

    override fun getNow(): T? = result
}