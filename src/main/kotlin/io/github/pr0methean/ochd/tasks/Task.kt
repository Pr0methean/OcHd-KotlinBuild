package io.github.pr0methean.ochd.tasks

import org.apache.logging.log4j.util.StringBuilderFormattable

interface Task<out T> : StringBuilderFormattable {
    val name: String
    fun isComplete(): Boolean
    fun isStarted(): Boolean
    fun isFailed(): Boolean
    fun isSucceeded() = isComplete() && !isFailed()

    fun dependencies(): Collection<Task<*>>
    suspend fun run(): T

    suspend fun join(): Result<T>

    fun reset() {
        for (task in dependencies()) {
            if (isSucceeded() || task.isFailed()) {
                task.reset()
            }
        }
        clearResult()
    }

    fun clearResult()
}