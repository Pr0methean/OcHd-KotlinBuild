package io.github.pr0methean.ochd.tasks.consumable

import kotlinx.coroutines.Deferred
import org.apache.logging.log4j.util.StringBuilderFormattable

interface ConsumableTask<T>: StringBuilderFormattable {
    val name: String
    suspend fun <R> consumeAsync(block: suspend (Result<T>) -> R): Deferred<R>

    fun getNow(): Result<T>?

    suspend fun startAsync(): Deferred<Result<T>>

    suspend fun await(): Result<T>

    suspend fun clearFailure()
    suspend fun checkSanity()
    suspend fun mergeWithDuplicate(other: ConsumableTask<T>): ConsumableTask<T>
}