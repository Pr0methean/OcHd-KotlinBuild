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

class QuadtreeImageNode(width: Int, height: Int, val topLeft: ImageNode,
                        val topRight: ImageNode, val bottomLeft: ImageNode, val bottomRight: ImageNode,
                        name: String, scope: CoroutineScope, retryer: Retryer, stats: ImageProcessingStats)
        : ImageNode(width, height, null, name, scope, retryer, stats) {
    class QuadtreePixelReader(private val treeNode: QuadtreeImageNode): AbstractPixelReader() {
        private fun getQuadrantReader(quadrant: Quadrant) = runBlocking { quadrant.getter(treeNode).pixelReader() }

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

        override fun getPixelFormat(): PixelFormat<*> = getQuadrantReader(Quadrant.UPPER_LEFT).pixelFormat

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
                runBlocking { treeNode.unpacked().pixelReader }.getPixels(
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
                runBlocking { treeNode.unpacked().pixelReader }.getPixels(
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
                runBlocking { treeNode.unpacked().pixelReader }.getPixels(
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

    override val isSolidColor: Boolean by lazy {
        topLeft.isSolidColor && topRight.isSolidColor && bottomLeft.isSolidColor && bottomRight.isSolidColor
                && super.isSolidColor
    }
}