package io.github.pr0methean.ochd.tasks.consumable.caching

/** Coroutine-safe. */
interface TaskCache<T> {
    fun getNow(): Result<T>?

    fun set(value: Result<T>?)
}