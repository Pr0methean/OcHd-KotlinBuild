package io.github.pr0methean.ochd.packedimage

import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.Retryer
import javafx.scene.image.*
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
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

class SolidColorImageNode(val color: Color, width: Int, height: Int,
        name: String, scope: CoroutineScope, retryer: Retryer, stats: ImageProcessingStats)
        : ImageNode(width, height, null, name, scope, retryer, stats) {
    private val argb = colorToArgb(color)

    inner class SolidColorPixelReader(val color: Color, val argb: Int): AbstractPixelReader() {
        override fun getPixelFormat(): PixelFormat<*> = PixelFormat.getIntArgbInstance()

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
            runBlocking { unpacked() }.pixelReader.getPixels(x, y, w, h, pixelformat, buffer, offset, scanlineStride)
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
            runBlocking { unpacked() }.pixelReader.getPixels(x, y, w, h, pixelformat, buffer, offset, scanlineStride)
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
            runBlocking { unpacked() }.pixelReader.getPixels(x, y, w, h, pixelformat, buffer, scanlineStride)
        }
    }

    override val isSolidColor = true

    override suspend fun toSolidColorIfPossible(): ImageNode = this

    override suspend fun asQuadtree(): QuadtreeImageNode {
        val quadrant = SolidColorImageNode(color, width / 2, height / 2, name, scope, retryer, stats)
        return QuadtreeImageNode(width, height, quadrant, quadrant, quadrant, quadrant, name, scope, retryer, stats)
    }

    override suspend fun pixelReader(): PixelReader {
        return SolidColorPixelReader(color, argb)
    }

    override suspend fun unpacked(): Image {
        val out = WritableImage(width, height)
        val writer = out.pixelWriter
        for (y in 0 until height) {
            for (x in 0 until width) {
                writer.setArgb(x, y, argb)
            }
        }
        return out
    }

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

    override fun toString(): String = "Solid[$color]"
}