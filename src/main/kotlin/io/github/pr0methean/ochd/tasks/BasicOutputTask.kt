package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.ImageProcessingContext
import javafx.embed.swing.SwingFXUtils
import kotlinx.coroutines.withContext
import javax.imageio.ImageIO


data class BasicOutputTask(
    private val producer: TextureTask,
    override val name: String,
    override val ctx: ImageProcessingContext
)
        : OutputTask(name, ctx) {
    override suspend fun invoke() {
        file.parentFile.mkdirs()
        withContext(ctx.ioDispatcher) {
            ImageIO.write(SwingFXUtils.fromFXImage(producer.getBitmap(), null), "png", file)
        }
    }
}