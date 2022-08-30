package io.github.pr0methean.ochd.tasks.caching

@Suppress("unused")
class StrongTaskCache<T>(name: String): AbstractTaskCache<T>(name) {
    @Volatile var result: Result<T>? = null
    override fun getNow(): Result<T>? = result

    override fun enabledSet(value: Result<T>?) {
        result = value
    }
}