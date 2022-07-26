package io.github.pr0methean.ochd.tasks

import org.apache.logging.log4j.util.StringBuilderFormattable

interface Task<T> : StringBuilderFormattable {
    val name: String
    fun isComplete(): Boolean
    fun isStarted(): Boolean
    fun isFailed(): Boolean
    fun isSucceeded() = isComplete() && !isFailed()
    suspend fun run(): T

    suspend fun join(): Result<T>
}