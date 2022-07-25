package io.github.pr0methean.ochd.tasks

import org.apache.logging.log4j.util.StringBuilderFormattable

interface Task : StringBuilderFormattable {
    val name: String
    fun isComplete(): Boolean
    fun isStarted(): Boolean
    fun dependencies(): Collection<Task>
    suspend fun run()
}