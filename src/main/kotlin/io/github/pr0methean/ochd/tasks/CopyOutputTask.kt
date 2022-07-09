package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.ImageProcessingContext

data class CopyOutputTask(
    private val baseTask: OutputTask,
    override val name: String,
    override val ctx: ImageProcessingContext
)
        : OutputTask(name, ctx) {
    override suspend fun invoke() {
        baseTask.await()
        baseTask.file.copyTo(file)
    }
}