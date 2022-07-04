package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.ImageProcessingContext
import javafx.embed.swing.SwingFXUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext
import java.io.File
import javax.imageio.ImageIO


data class BasicOutputTask(
    private val producer: TextureTask,
    override val file: File,
    val scope: CoroutineScope,
    override val ctx: ImageProcessingContext
)
        : OutputTask(scope, file, ctx) {
    override suspend fun invoke() {
        file.parentFile.mkdirs()
        withContext(ctx.ioDispatcher) {
            ImageIO.write(SwingFXUtils.fromFXImage(producer.getBitmap(), null), "png", file)
        }
    }
}