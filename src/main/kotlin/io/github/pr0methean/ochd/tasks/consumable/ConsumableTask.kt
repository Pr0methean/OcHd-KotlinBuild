package io.github.pr0methean.ochd.tasks.consumable

import kotlinx.coroutines.Deferred
import org.apache.logging.log4j.util.StringBuilderFormattable

interface ConsumableTask<T>: StringBuilderFormattable {
    val name: String
    suspend fun consume(block: suspend (Result<T>) -> Unit)

    fun getNow(): Result<T>?

    suspend fun startAsync(): Deferred<Result<T>>

    suspend fun await(): Result<T>

    suspend fun clearFailure()
}