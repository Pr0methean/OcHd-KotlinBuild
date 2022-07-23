package io.github.pr0methean.ochd.packedimage

import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.Retryer
import io.github.pr0methean.ochd.tasks.doJfx
import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.Image
import javafx.scene.image.PixelReader
import javafx.scene.image.WritableImage
import javafx.scene.image.WritablePixelFormat
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import kotlinx.coroutines.CoroutineScope
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.IntBuffer
import kotlin.math.roundToInt

fun colorToArgb(color: Color) = (color.opacity * 255.0).roundToInt().shl(24)
    .or(
        (color.red * 255.0).roundToInt().shl(16)
    )
    .or(
        (color.green * 255.0).roundToInt().shl(8)
    )
    .or((color.blue * 255.0).roundToInt())

private fun unpack(argb: Int, width: Int, height: Int): Image {
    val out = WritableImage(width, height)
    val writer = out.pixelWriter
    for (y in 0 until height) {
        for (x in 0 until width) {
            writer.setArgb(x, y, argb)
        }
    }
    return out
}

class SolidColorImageNode(val color: Color, width: Int, height: Int,
        name: String, scope: CoroutineScope, retryer: Retryer, stats: ImageProcessingStats)
        : ImageNode(width, height, initialPacked = null, name = name, scope = scope, retryer = retryer, stats = stats) {
    private val argb = colorToArgb(color)

    class SolidColorPixelReader(val width: Int, val height: Int, val color: Color, val argb: Int)
            : AbstractPixelReader(unpacked = {unpack(argb, width, height)}) {
        override fun getArgb(x: Int, y: Int): Int = argb

        override fun getColor(x: Int, y: Int): Color = color
        override fun getPixels(
            x: Int,
            y: Int,
            w: Int,
            h: Int,
            pixelformat: WritablePixelFormat<ByteBuffer>,
            buffer: ByteArray,
            offset: Int,
            scanlineStride: Int
        ) {
            sourceReader().getPixels(x, y, w, h, pixelformat, buffer, offset, scanlineStride)
        }

        override fun getPixels(
            x: Int,
            y: Int,
            w: Int,
            h: Int,
            pixelformat: WritablePixelFormat<IntBuffer>,
            buffer: IntArray,
            offset: Int,
            scanlineStride: Int
        ) {
            sourceReader().getPixels(x, y, w, h, pixelformat, buffer, offset, scanlineStride)
        }

        override fun <T : Buffer> getPixels(
            x: Int,
            y: Int,
            w: Int,
            h: Int,
            pixelformat: WritablePixelFormat<T>,
            buffer: T,
            scanlineStride: Int
        ) {
            sourceReader().getPixels(x, y, w, h, pixelformat, buffer, scanlineStride)
        }
    }

    override suspend fun isSolidColor() = true

    override suspend fun toSolidColorIfPossible(): ImageNode = this

    override suspend fun asQuadtree(): QuadtreeImageNode {
        val quadrant = SolidColorImageNode(color, width / 2, height / 2, name, scope, retryer, stats)
        return QuadtreeImageNode(width, height, quadrant, quadrant, quadrant, quadrant, name, scope, retryer, stats)
    }

    override suspend fun pixelReader(): PixelReader {
        return SolidColorPixelReader(width, height, color, argb)
    }

    override suspend fun unpack(): Image = unpack(argb, width, height)

    override suspend fun repaint(
        newPaint: Paint?,
        alpha: Double,
        name: String,
        retryer: Retryer,
        packer: ImagePacker
    ): ImageNode {
        val newPaintWithAlpha = if (newPaint is Color) {
            if (alpha == 1.0 && color.opacity == 1.0) {
                newPaint
            } else if (color.opacity == 0.0 || alpha == 0.0 || newPaint.opacity == 0.0) {
                Color.TRANSPARENT
            } else {
                Color(newPaint.red, newPaint.green, newPaint.blue, newPaint.opacity * alpha * color.opacity)
            }
        } else if (newPaint == null) {
            if (alpha == 1.0) {
                return this
            }
            Color(color.red, color.green, color.blue, color.opacity * alpha)
        } else return super.repaint(newPaint, alpha, name, retryer, packer)
        return SolidColorImageNode(newPaintWithAlpha, width, height, name, scope, retryer, stats)
    }

    override suspend fun renderTo(out: GraphicsContext, x: Int, y: Int) {
        doJfx(name, retryer) {
            out.fill = color
            out.fillRect(x.toDouble(), y.toDouble(), (x + width).toDouble(), (y + height).toDouble())
        }
    }
}