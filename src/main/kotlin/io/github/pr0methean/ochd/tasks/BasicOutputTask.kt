package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.ImageProcessingContext


data class BasicOutputTask(
    private val producer: TextureTask,
    override val name: String,
    override val ctx: ImageProcessingContext
)
        : OutputTask(name, ctx) {
    override suspend fun invoke() {
        file.parentFile.mkdirs()
        try {
            producer.getImage().writePng(file)
        } catch (e: NotImplementedError) {
            println("Skipping $name because it's not implemented yet")
        }
    }
}