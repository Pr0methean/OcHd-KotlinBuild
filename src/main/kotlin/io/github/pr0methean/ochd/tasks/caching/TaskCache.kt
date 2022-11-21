package io.github.pr0methean.ochd.tasks.caching

/** Coroutine-safe. */
interface TaskCache<T> {
    var enabled: Boolean
    fun getNow(): T?
    fun set(value: T?)
}