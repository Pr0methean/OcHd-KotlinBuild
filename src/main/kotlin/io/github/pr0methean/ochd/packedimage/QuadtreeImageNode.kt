package io.github.pr0methean.ochd.packedimage

import io.github.pr0methean.ochd.AsyncLazy
import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.Retryer
import io.github.pr0methean.ochd.SoftAsyncLazy
import javafx.scene.image.Image
import javafx.scene.image.PixelReader
import javafx.scene.image.WritableImage
import javafx.scene.image.WritablePixelFormat
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.IntBuffer
import java.util.*

class QuadtreeImageNode(width: Int, height: Int, val topLeft: ImageNode,
                        val topRight: ImageNode, val bottomLeft: ImageNode, val bottomRight: ImageNode,
                        name: String, scope: CoroutineScope, retryer: Retryer, stats: ImageProcessingStats)
        : ImageNode(width, height, null, name, scope, retryer, stats) {
    class QuadtreePixelReader(private val treeNode: QuadtreeImageNode): AbstractPixelReader(unpacked = {treeNode.unpacked()}) {
        private val quadrantReaders = EnumMap<Quadrant, SoftAsyncLazy<PixelReader>>(Quadrant::class.java).also {
            for (quadrant in enumValues<Quadrant>()) {
                it[quadrant] = SoftAsyncLazy {quadrant.getter(treeNode).pixelReader()}
            }
        }
        private fun getQuadrantReader(quadrant: Quadrant) = runBlocking {quadrantReaders[quadrant]!!.get()}

        private fun quadrantForPoint(x: Int, y: Int): Triple<Quadrant, Int, Int> =
            if (x < treeNode.tileWidth) {
                if (y < treeNode.tileHeight) {
                    Triple(Quadrant.UPPER_LEFT, x, y)
                } else {
                    Triple(Quadrant.LOWER_LEFT, x, y - treeNode.tileHeight)
                }
            } else {
                if (y < treeNode.tileHeight) {
                    Triple(Quadrant.UPPER_RIGHT, x - treeNode.tileWidth, y)
                } else {
                    Triple(Quadrant.LOWER_RIGHT, x - treeNode.tileWidth, y - treeNode.tileHeight)
                }
            }

        override fun getArgb(x: Int, y: Int): Int {
            val (quadrant, qx, qy) = quadrantForPoint(x, y)
            return getQuadrantReader(quadrant).getArgb(qx, qy)
        }

        override fun getColor(x: Int, y: Int): Color {
            val (quadrant, qx, qy) = quadrantForPoint(x, y)
            return getQuadrantReader(quadrant).getColor(qx, qy)
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
            val (quadrantTopLeft, qLeft, qTop) = quadrantForPoint(x, y)
            val (quadrantBottomRight, _, _) = quadrantForPoint(x + w, y + h)
            if (quadrantTopLeft == quadrantBottomRight) {
                getQuadrantReader(quadrantTopLeft).getPixels(qLeft, qTop, w, h, pixelformat, buffer, scanlineStride)
            } else {
                sourceReader().getPixels(
                    x,
                    y,
                    w,
                    h,
                    pixelformat,
                    buffer,
                    scanlineStride
                )
            }
        }

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
            val (quadrantTopLeft, qLeft, qTop) = quadrantForPoint(x, y)
            val (quadrantBottomRight, _, _) = quadrantForPoint(x + w, y + h)
            if (quadrantTopLeft == quadrantBottomRight) {
                getQuadrantReader(quadrantTopLeft).getPixels(
                    qLeft,
                    qTop,
                    w,
                    h,
                    pixelformat,
                    buffer,
                    offset,
                    scanlineStride
                )
            } else {
                sourceReader().getPixels(
                    x,
                    y,
                    w,
                    h,
                    pixelformat,
                    buffer,
                    offset,
                    scanlineStride
                )
            }
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
            val (quadrantTopLeft, qLeft, qTop) = quadrantForPoint(x, y)
            val (quadrantBottomRight, _, _) = quadrantForPoint(x + w, y + h)
            if (quadrantTopLeft == quadrantBottomRight) {
                getQuadrantReader(quadrantTopLeft).getPixels(
                    qLeft,
                    qTop,
                    w,
                    h,
                    pixelformat,
                    buffer,
                    offset,
                    scanlineStride
                )
            } else {
                sourceReader().getPixels(
                    x,
                    y,
                    w,
                    h,
                    pixelformat,
                    buffer,
                    offset,
                    scanlineStride
                )
            }
        }
    }

    override suspend fun pixelReader(): PixelReader = QuadtreePixelReader(this)

    enum class Quadrant(val xMultiplier: Int, val yMultiplier: Int, val getter: (QuadtreeImageNode) -> ImageNode) {
        UPPER_LEFT(0,0, QuadtreeImageNode::topLeft),
        UPPER_RIGHT(1,0, QuadtreeImageNode::topRight),
        LOWER_LEFT(0,1, QuadtreeImageNode::bottomLeft),
        LOWER_RIGHT(1,1, QuadtreeImageNode::bottomRight);
        fun getLeftX(parentWidth: Int) = xMultiplier * (parentWidth / 2)
        fun getTopY(parentHeight: Int) = yMultiplier * (parentHeight / 2)
    }
    val tileWidth = width / 2
    val tileHeight = height / 2
    override suspend fun asQuadtree(): QuadtreeImageNode = this
    override suspend fun unpacked(): Image {
        val out = WritableImage(width, height)
        val writer = out.pixelWriter
        for (quadrant in enumValues<Quadrant>()) {
            writer.setPixels(quadrant.getLeftX(width), quadrant.getTopY(height), tileWidth, tileHeight,
                quadrant.getter(this).unpacked().pixelReader, 0, 0)
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
        return QuadtreeImageNode(width, height,
            topLeft = topLeft.repaint(newPaint, alpha, name, retryer, packer),
            topRight = topRight.repaint(newPaint, alpha, name, retryer, packer),
            bottomLeft = bottomLeft.repaint(newPaint, alpha, name, retryer, packer),
            bottomRight = bottomRight.repaint(newPaint, alpha, name, retryer, packer),
        name, scope, retryer, stats)
    }

    override fun toString(): String = "Quadtree[UL=$topLeft,UR=$topRight,LL=$bottomLeft,LR=$bottomRight]"

    override val isSolidColor = AsyncLazy {
        if (!topLeft.isSolidColor()) {return@AsyncLazy false}
        if (!topRight.isSolidColor()) {return@AsyncLazy false}
        if (!bottomLeft.isSolidColor()) {return@AsyncLazy false}
        if (!bottomRight.isSolidColor()) {return@AsyncLazy false}
        val topLeftColor = topLeft.pixelReader().getArgb(0,0)
        val topRightColor = topRight.pixelReader().getArgb(0,0)
        if (topRightColor != topLeftColor) {return@AsyncLazy false}
        val bottomLeftColor = bottomLeft.pixelReader().getArgb(0,0)
        if (bottomLeftColor != topLeftColor) {return@AsyncLazy false}
        val bottomRightColor = bottomRight.pixelReader().getArgb(0, 0)
        if (bottomRightColor != topLeftColor) {return@AsyncLazy false}
        if (bottomRightColor != topLeftColor) {return@AsyncLazy false}
        true
    }
}