package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.ImageProcessingContext
import java.io.File

data class CopyOutputTask(
    private val baseTask: OutputTask,
    override val file: File,
    override val ctx: ImageProcessingContext
)
        : OutputTask(file, ctx) {
    override suspend fun invoke() {
        baseTask.run()
        baseTask.file.copyTo(file)
    }
}