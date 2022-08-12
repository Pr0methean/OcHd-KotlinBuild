package io.github.pr0methean.ochd.tasks.consumable.caching

@Suppress("unused")
class StrongTaskCache<T>: AbstractTaskCache<T>() {
    @Volatile var result: Result<T>? = null
    override fun getNow(): Result<T>? = result

    override fun enabledSet(value: Result<T>?) {
        result = value
    }
}