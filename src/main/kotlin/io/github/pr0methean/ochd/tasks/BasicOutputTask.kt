package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.ImageProcessingContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


data class BasicOutputTask(
    private val producer: TextureTask<*>,
    override val name: String,
    override val ctx: ImageProcessingContext
)
        : OutputTask(name, ctx) {
    override suspend fun invoke() {
        file.parentFile.mkdirs()
        withContext(Dispatchers.IO) {
            producer.getPackedImage().writePng(file)
        }
    }
}