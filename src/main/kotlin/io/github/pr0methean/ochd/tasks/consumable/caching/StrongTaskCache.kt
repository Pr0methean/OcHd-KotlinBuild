package io.github.pr0methean.ochd.tasks.consumable.caching

class StrongTaskCache<T>: TaskCache<T> {
    @Volatile var result: Result<T>? = null
    override fun getNow(): Result<T>? = result

    override fun set(value: Result<T>?) {
        result = value
    }
}