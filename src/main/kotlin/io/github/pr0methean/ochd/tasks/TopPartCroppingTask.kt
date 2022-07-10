package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.ImageProcessingContext
import javafx.scene.image.Image
import javafx.scene.image.WritableImage

const val TOP_PORTION = 11.0/32
data class TopPartCroppingTask(
    val base: TextureTask,
    val width: Int,
    override val ctx: ImageProcessingContext
): TextureTask(ctx) {
    private val height = (width * TOP_PORTION).toInt()
    override suspend fun computeImage(): Image {
        val pixelReader = base.await().unpacked().pixelReader
        return WritableImage(pixelReader, width, height)
    }
}