package io.github.pr0methean.ochd.packedimage

import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.Retryer
import io.github.pr0methean.ochd.SoftAsyncLazy
import io.github.pr0methean.ochd.StrongAsyncLazy
import io.github.pr0methean.ochd.tasks.doJfx
import javafx.scene.canvas.GraphicsContext
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

class QuadtreeImageNode(
    initialUnpacked: Image? = null, width: Int, height: Int, val topLeft: ImageNode,
    val topRight: ImageNode, val bottomLeft: ImageNode, val bottomRight: ImageNode,
    name: String, scope: CoroutineScope, retryer: Retryer, stats: ImageProcessingStats, packer: ImagePacker
)
        : ImageNode(width, height, initialUnpacked = initialUnpacked, initialPacked = null, name = name, scope = scope, retryer = retryer, stats = stats,
        packer = packer) {
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

    override val pixelReader = SoftAsyncLazy<PixelReader>(initialUnpacked?.pixelReader) {
        QuadtreePixelReader(this)
    }

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
    override suspend fun unpack(): Image {
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
        val repainted = if (newPaint == null && alpha == 1.0) {
            this
        } else {
            QuadtreeImageNode(
                null, width, height,
                topLeft = topLeft.repaint(newPaint, alpha, name, retryer, packer),
                topRight = topRight.repaint(newPaint, alpha, name, retryer, packer),
                bottomLeft = bottomLeft.repaint(newPaint, alpha, name, retryer, packer),
                bottomRight = bottomRight.repaint(newPaint, alpha, name, retryer, packer),
                name, scope, retryer, stats, packer)
        }
        return packer.deduplicate(repainted)
    }

    override fun shouldDeduplicate(): Boolean = topLeft.shouldDeduplicate() && topRight.shouldDeduplicate()
            && bottomLeft.shouldDeduplicate() && bottomRight.shouldDeduplicate()

    override suspend fun renderTo(out: GraphicsContext, x: Int, y: Int) {
        val rendered = unpacked.getNow()
        if (rendered != null) {
            doJfx(name, retryer) {out.drawImage(rendered, x.toDouble(), y.toDouble())}
        }
        for (quadrant in enumValues<Quadrant>()) {
            quadrant.getter(this).renderTo(out, quadrant.getLeftX(width) + x, quadrant.getTopY(height) + y)
        }
    }

    override val isSolidColor = StrongAsyncLazy {
        if (!topLeft.isSolidColor()) {return@StrongAsyncLazy false}
        if (!topRight.isSolidColor()) {return@StrongAsyncLazy false}
        if (!bottomLeft.isSolidColor()) {return@StrongAsyncLazy false}
        if (!bottomRight.isSolidColor()) {return@StrongAsyncLazy false}
        val topLeftColor = topLeft.pixelReader().getArgb(0,0)
        val topRightColor = topRight.pixelReader().getArgb(0,0)
        if (topRightColor != topLeftColor) {return@StrongAsyncLazy false}
        val bottomLeftColor = bottomLeft.pixelReader().getArgb(0,0)
        if (bottomLeftColor != topLeftColor) {return@StrongAsyncLazy false}
        val bottomRightColor = bottomRight.pixelReader().getArgb(0, 0)
        if (bottomRightColor != topLeftColor) {return@StrongAsyncLazy false}
        if (bottomRightColor != topLeftColor) {return@StrongAsyncLazy false}
        true
    }

    override fun equals(other: Any?): Boolean {
        return (this === other) || (other is QuadtreeImageNode
                && width == other.width
                && height == other.height
                && topLeft == other.topLeft
                && topRight == other.topRight
                && bottomLeft == other.bottomLeft
                && bottomRight == other.bottomRight)
    }

    override fun hashCode(): Int {
        return Objects.hash(width, height, topLeft, topRight, bottomLeft, bottomRight)
    }
}