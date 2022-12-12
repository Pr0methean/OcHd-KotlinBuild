package io.github.pr0methean.ochd.tasks

import org.apache.logging.log4j.util.StringBuilderFormattable

/**
 * Unit of work that wraps its coroutine to support reuse (including under heap-constrained conditions) and retrying.
 */
interface Task<out T>: StringBuilderFormattable {
    val name: String

    fun getNow(): T?

    suspend fun mergeWithDuplicate(other: Task<*>): Task<T>

    suspend fun addDirectDependentTask(task: Task<*>)

    suspend fun removeDirectDependentTask(task: Task<*>)

    fun cacheableSubtasks(): Int

    fun startedOrAvailableSubtasks(): Int

    fun isStartedOrAvailable(): Boolean

    suspend fun registerRecursiveDependencies()

    val directDependencies: Iterable<Task<*>>

    val totalSubtasks: Int

    fun timesFailed(): Long
    suspend fun await(): T
    fun clearCache()
}
