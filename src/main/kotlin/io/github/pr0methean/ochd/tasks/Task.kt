package io.github.pr0methean.ochd.tasks

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import org.apache.logging.log4j.util.StringBuilderFormattable

interface Task<T>: StringBuilderFormattable {
    val name: String

    fun getNow(): Result<T>?

    suspend fun startAsync(): Deferred<Result<T>>

    suspend fun clearFailure()
    suspend fun mergeWithDuplicate(other: Task<T>): Task<T>

    fun addDirectDependentTask(task: Task<*>)

    fun removeDirectDependentTask(task: Task<*>)

    fun uncachedCacheableSubtasks(): Int = if (getNow() != null) {
        0
    } else {
        var total = 1
        for (task in directDependencies) {
            total += task.uncachedCacheableSubtasks()
        }
        total
    }

    fun cachedSubtasks(): Int

    fun isCachingEnabled(): Boolean

    fun registerRecursiveDependencies()

    val directDependencies: Iterable<Task<*>>

    suspend fun createCoroutineScope(): CoroutineScope = CoroutineScope(
        currentCoroutineContext()
            .plus(CoroutineName(name))
            .plus(SupervisorJob())
    )
}

@Suppress("DeferredResultUnused")
suspend inline fun <T, R> Task<T>.consumeAsync(crossinline block: suspend (Result<T>) -> R): Deferred<R>
        = createCoroutineScope().async {
            block(await())
        }

suspend inline fun <T> Task<T>.await(): Result<T> = startAsync().await()