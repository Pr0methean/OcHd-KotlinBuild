package io.github.pr0methean.ochd.tasks

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import org.apache.logging.log4j.util.StringBuilderFormattable

/**
 * Cacheable, retryable unit of work.
 */
interface Task<out T>: StringBuilderFormattable {
    val name: String

    fun getNow(): Result<T>?

    suspend fun startAsync(): Deferred<Result<T>>

    suspend fun mergeWithDuplicate(other: Task<*>): Task<T>

    suspend fun addDirectDependentTask(task: Task<*>)

    suspend fun removeDirectDependentTask(task: Task<*>)

    fun cacheableSubtasks(): Int

    fun startedOrAvailableSubtasks(): Int

    fun isStartedOrAvailable(): Boolean

    suspend fun registerRecursiveDependencies()

    val directDependencies: Iterable<Task<*>>

    suspend fun getCoroutineScope(): CoroutineScope

    val totalSubtasks: Int

    fun timesFailed(): Long
}

suspend inline fun <T> Task<T>.await(): Result<T> = startAsync().await()
