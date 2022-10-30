package io.github.pr0methean.ochd.tasks

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import org.apache.logging.log4j.util.StringBuilderFormattable

interface Task<T>: StringBuilderFormattable {
    val name: String

    fun getNow(): Result<T>?

    suspend fun startAsync(): Deferred<Result<T>>

    suspend fun clearFailure()
    suspend fun mergeWithDuplicate(other: Task<T>): Task<T>

    suspend fun addDirectDependentTask(task: Task<*>)

    suspend fun removeDirectDependentTask(task: Task<*>)

    fun unstartedCacheableSubtasks(): Int = if (isStartedOrAvailable() || !isCachingEnabled()) {
        0
    } else {
        var total = 1
        for (task in directDependencies) {
            total += task.unstartedCacheableSubtasks()
        }
        total
    }

    fun cachedSubtasks(): Int

    fun isCachingEnabled(): Boolean

    fun isStartedOrAvailable(): Boolean

    suspend fun registerRecursiveDependencies()

    val directDependencies: Iterable<Task<*>>

    suspend fun createCoroutineScope(): CoroutineScope

    val totalSubtasks: Int

    fun timesFailed(): Long
}

@Suppress("DeferredResultUnused")
suspend inline fun <T, R> Task<T>.consumeAsync(crossinline block: suspend (Result<T>) -> R): Deferred<R>
        = createCoroutineScope().async {
            block(await())
        }

suspend inline fun <T> Task<T>.await(): Result<T> = startAsync().await()