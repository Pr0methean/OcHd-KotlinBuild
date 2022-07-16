package io.github.pr0methean.ochd.packedimage

import io.github.pr0methean.ochd.DEFAULT_SNAPSHOT_PARAMS
import io.github.pr0methean.ochd.ImageProcessingContext
import io.github.pr0methean.ochd.tasks.doJfx
import javafx.scene.canvas.Canvas
import javafx.scene.image.Image
import javafx.scene.image.WritableImage
import javafx.scene.paint.Color
import javafx.scene.paint.Paint

fun toArgb(color: Color) = (256 * color.opacity).toInt().shl(24)
        .or((256 * color.red).toInt().shl(16))
        .or((256 * color.green).toInt().shl(8))
        .or((256 * color.blue).toInt())

data class SolidTile(val paint: Paint, val width: Int, val height: Int, val ctx: ImageProcessingContext): ImageTile {
    override suspend fun asImage(): Image {
        if (paint is Color) {
            val image = WritableImage(width, height)
            val argb = toArgb(paint)
            val writer = image.pixelWriter
            for (y in 0 until height) {
                for (x in 0 until width) {
                    writer.setArgb(x, y, argb)
                }
            }
            return image
        } else {
            val canvas = Canvas(width.toDouble(), height.toDouble())
            return doJfx("Render a solid tile with $paint", ctx) {
                val gfx = canvas.graphicsContext2D
                gfx.fill = paint
                gfx.fillRect(0.0, 0.0, width.toDouble(), height.toDouble())
                canvas.snapshot(DEFAULT_SNAPSHOT_PARAMS, null)
            }
        }
    }

    override suspend fun repainted(paint: Paint, alpha: Double): ImageTile = SolidTile(paint, width, height, ctx)
    override suspend fun drawOnCanvas(canvas: Canvas, x: Int, y: Int) {
        canvas.graphicsContext2D.fillRect(x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble())
    }

}