package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.ImageProcessingContext
import java.io.File

abstract class OutputTask(open val file: File, open val ctx: ImageProcessingContext) {
    protected abstract suspend fun invoke()
    suspend fun run() {
        ctx.taskLaunches.add(this::class.simpleName)
        println("Starting output task for $file")
        invoke()
        println("Finished output task for $file")
    }
}