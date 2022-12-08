package io.github.pr0methean.ochd.tasks.caching

/**
 * Stores the output of a particular [io.github.pr0methean.ochd.tasks.Task] for reuse, but can usually be cleared if the
 * memory is needed. May be backed by an evictable entry in a Caffeine cache and/or a soft or weak reference. A separate
 * instance is needed for each Task (unless it uses [NoopTaskCache]), and that Task should be the only object that
 * references it strongly.
 */
interface TaskCache<T> {
    var enabled: Boolean
    val name: String
    fun getNow(): T?
    fun set(value: T?)
    fun disable()
    fun clear()
    fun enabledSet(value: T)
}
