package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.ImageProcessingContext
import javafx.scene.image.Image
import javafx.scene.image.PixelReader
import javafx.scene.image.WritableImage
import javafx.scene.paint.Color

const val TOP_PORTION = 11.0/32
data class TopPartCroppingTask(
    val base: TextureTask<*>,
    val width: Int,
    override val ctx: ImageProcessingContext
): TextureTask<PixelReader>(ctx) {
    private val height = (width * TOP_PORTION).toInt()
    override suspend fun computeInput(): PixelReader = base.getImage().pixelReader

    override fun doBlockingJfx(input: PixelReader): Image {
        val writableImage = WritableImage(width, width)
        val pixelWriter = writableImage.pixelWriter

        for (y in 0 until height) {
            for (x in 0 until width) {
                val color: Color = input.getColor(x, y)
                pixelWriter.setColor(x, y, color)
            }
        }
        return writableImage
    }
}