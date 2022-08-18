package io.github.pr0methean.ochd.tasks

import kotlinx.coroutines.Deferred
import org.apache.logging.log4j.util.StringBuilderFormattable

interface Task<T>: StringBuilderFormattable {
    val name: String
    suspend fun <R> consumeAsync(block: suspend (Result<T>) -> R): Deferred<R>

    fun getNow(): Result<T>?

    suspend fun startAsync(): Deferred<Result<T>>

    suspend fun await(): Result<T>

    suspend fun clearFailure()
    suspend fun mergeWithDuplicate(other: Task<T>): Task<T>

    fun addDependentOutputTask(task: OutputTask)

    fun removeDependentOutputTask(task: OutputTask)
}