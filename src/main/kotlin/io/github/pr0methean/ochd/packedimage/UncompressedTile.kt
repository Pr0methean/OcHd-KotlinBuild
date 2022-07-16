package io.github.pr0methean.ochd.packedimage

import io.github.pr0methean.ochd.ImageProcessingContext
import io.github.pr0methean.ochd.tasks.ImmediateTextureTask
import io.github.pr0methean.ochd.tasks.RepaintTask
import javafx.scene.canvas.Canvas
import javafx.scene.image.Image
import javafx.scene.paint.Paint

class UncompressedTile(val image: Image, val ctx: ImageProcessingContext): ImageTile {
    override suspend fun asImage(): Image = image
    override suspend fun repainted(paint: Paint, alpha: Double) =
        UncompressedTile(RepaintTask(paint, ImmediateTextureTask(UncompressedImage(image, "tile", ctx)), image.width.toInt(), alpha, ctx).getImage().unpacked(), ctx)

    override suspend fun drawOnCanvas(canvas: Canvas, x: Int, y: Int) {
        canvas.graphicsContext2D.drawImage(image, x.toDouble(), y.toDouble())
    }
}