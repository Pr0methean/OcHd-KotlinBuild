package io.github.pr0methean.ochd.tasks

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import org.apache.logging.log4j.util.StringBuilderFormattable

interface Task<out T>: StringBuilderFormattable {
    val name: String

    fun getNow(): Result<T>?

    suspend fun startAsync(): Deferred<Result<T>>

    suspend fun clearFailure()
    suspend fun mergeWithDuplicate(other: Task<*>): Task<T>

    suspend fun addDirectDependentTask(task: Task<*>)

    suspend fun removeDirectDependentTask(task: Task<*>)

    fun unstartedCacheableSubtasks(): Collection<Task<*>> = if (isStartedOrAvailable() || !isCachingEnabled()) {
        listOf()
    } else {
        val subtasks = mutableSetOf<Task<*>>(this)
        for (task in directDependencies) {
            subtasks += task.unstartedCacheableSubtasks()
        }
        subtasks
    }

    fun cachedSubtasks(): Set<Task<*>>

    fun isCachingEnabled(): Boolean

    fun isStartedOrAvailable(): Boolean

    suspend fun registerRecursiveDependencies()

    val directDependencies: Iterable<Task<*>>

    suspend fun createCoroutineScope(): CoroutineScope

    val totalSubtasks: Int

    fun timesFailed(): Long
    fun andAllSubtasks(): Set<Task<*>>
}

@Suppress("DeferredResultUnused")
suspend inline fun <T, R> Task<T>.consumeAsync(crossinline block: suspend (Result<T>) -> R): Deferred<R>
        = createCoroutineScope().async {
            block(await())
        }

suspend inline fun <T> Task<T>.await(): Result<T> = startAsync().await()