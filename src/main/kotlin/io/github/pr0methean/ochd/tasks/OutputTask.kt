package io.github.pr0methean.ochd.tasks

import kotlinx.coroutines.CoroutineScope
import java.io.File

abstract class OutputTask(private val scope: CoroutineScope, open val file: File) {
    protected abstract suspend fun invoke()
    suspend fun run() {
        println("Starting output task for $file")
        invoke()
        println("Finished output task for $file")
    }
}