package io.github.pr0methean.ochd.tasks.caching

/** Coroutine-safe. */
interface TaskCache<T> {
    var enabled: Boolean
    val name: String
    fun getNow(): T?
    fun set(value: T?)
    fun disable()
    fun clear()
    fun enabledSet(value: T)
}
