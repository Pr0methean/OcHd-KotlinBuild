package io.github.pr0methean.ochd.tasks.caching

/** Coroutine-safe. */
interface TaskCache<T> {
    fun enable()
    fun disable()
    fun getNow(): Result<T>?
    fun set(value: Result<T>?)
}