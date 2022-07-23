package io.github.pr0methean.ochd.packedimage

import io.github.pr0methean.ochd.DEFAULT_SNAPSHOT_PARAMS
import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.Retryer
import io.github.pr0methean.ochd.packedimage.QuadtreeImageNode.Quadrant
import io.github.pr0methean.ochd.tasks.doJfx
import javafx.embed.swing.SwingFXUtils
import javafx.scene.CacheHint
import javafx.scene.canvas.Canvas
import javafx.scene.effect.Blend
import javafx.scene.effect.BlendMode
import javafx.scene.effect.ColorInput
import javafx.scene.image.*
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import kotlinx.coroutines.*
import org.apache.logging.log4j.LogManager
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.IntBuffer
import java.util.*
import javax.imageio.ImageIO

private val logger = LogManager.getLogger("ImageNode")
abstract class ImageNode(val width: Int, val height: Int, initialPacked: ByteArray? = null, val name: String,
                         val scope: CoroutineScope, val retryer: Retryer, val stats: ImageProcessingStats,
                         start: CoroutineStart = CoroutineStart.LAZY) {

    protected val packingTask by lazy {
        if (initialPacked == null) {
            scope.async(start = start) {
                ByteArrayOutputStream().use {
                    retryer.retrying<ByteArray>("Compression of $name") {
                        stats.onCompressPngImage(name)
                        @Suppress("BlockingMethodInNonBlockingContext")
                        ImageIO.write(SwingFXUtils.fromFXImage(unpacked(), null), "PNG", it)
                        return@retrying it.toByteArray()
                    }
                }.also { logger.info("Done compressing {}", name) }
            }
        } else CompletableDeferred(initialPacked)
    }

    class ImageNodePixelReader(val unpacked: suspend () -> Image) : PixelReader {
        private fun blockForSource() = runBlocking { unpacked() }
        override fun getPixelFormat(): PixelFormat<*> = blockForSource().pixelReader.pixelFormat

        override fun getArgb(x: Int, y: Int): Int = blockForSource().pixelReader.getArgb(x, y)

        override fun getColor(x: Int, y: Int): Color = blockForSource().pixelReader.getColor(x, y)

        override fun <T : Buffer?> getPixels(
            x: Int,
            y: Int,
            w: Int,
            h: Int,
            pixelformat: WritablePixelFormat<T>?,
            buffer: T,
            scanlineStride: Int
        ) {
            blockForSource().pixelReader.getPixels(x, y, w, h, pixelformat, buffer, scanlineStride)
        }

        override fun getPixels(
            x: Int,
            y: Int,
            w: Int,
            h: Int,
            pixelformat: WritablePixelFormat<ByteBuffer>?,
            buffer: ByteArray?,
            offset: Int,
            scanlineStride: Int
        ) {
            blockForSource().pixelReader.getPixels(x, y, w, h, pixelformat, buffer, offset, scanlineStride)
        }

        override fun getPixels(
            x: Int,
            y: Int,
            w: Int,
            h: Int,
            pixelformat: WritablePixelFormat<IntBuffer>?,
            buffer: IntArray?,
            offset: Int,
            scanlineStride: Int
        ) {
            blockForSource().pixelReader.getPixels(x, y, w, h, pixelformat, buffer, offset, scanlineStride)
        }

    }

    open val isSolidColor: Boolean by lazy {
        val pixelReader = runBlocking { pixelReader() }
        val topLeftArgb = pixelReader.getArgb(0, 0)
        for (y in 0 until height) {
            for (x in 0 until width) {
                if (pixelReader.getArgb(x, y) != topLeftArgb) {
                    return@lazy false
                }
            }
        }
        return@lazy true
    }

    open suspend fun toSolidColorIfPossible(): ImageNode {
        return if (isSolidColor) {
            SolidColorImageNode(pixelReader().getColor(0, 0), width, height, name, scope, retryer, stats)
        } else {
            this
        }
    }

    open suspend fun asQuadtree(): QuadtreeImageNode {
        val pixelReader = pixelReader()
        val tileWidth = width / 2
        val tileHeight = height / 2
        val quadrants = EnumMap<Quadrant, ImageNode>(Quadrant::class.java)
        for (quadrant in enumValues<Quadrant>()) {
            val quadrantBuffer = WritableImage(tileWidth, tileHeight)
            quadrantBuffer.pixelWriter.setPixels(
                0, 0, tileWidth, tileHeight, pixelReader, quadrant.getLeftX(width),
                quadrant.getTopY(height)
            )
            quadrants[quadrant] =
                SimpleImageNode(quadrantBuffer, null, name, scope, retryer, stats, tileWidth, tileHeight)
        }
        return QuadtreeImageNode(
            width, height,
            topLeft = quadrants[Quadrant.UPPER_LEFT]!!.toSolidColorIfPossible(),
            topRight = quadrants[Quadrant.UPPER_RIGHT]!!.toSolidColorIfPossible(),
            bottomLeft = quadrants[Quadrant.LOWER_LEFT]!!.toSolidColorIfPossible(),
            bottomRight = quadrants[Quadrant.LOWER_RIGHT]!!.toSolidColorIfPossible(),
            name, scope, retryer, stats)
    }

    open suspend fun asSolidOrQuadtreeRecursive(
        maxDepth: Int,
        leafWidth: Int,
        leafHeight: Int
    ): ImageNode {
        if (width <= leafWidth || height <= leafHeight) {
            return this
        }
        val solid = toSolidColorIfPossible()
        if (solid is SolidColorImageNode) {
            return solid
        }
        if (maxDepth == 0) {
            return this
        }
        if (maxDepth == 1) {
            return asQuadtree()
        }
        val result = asQuadtree()
        val topLeft = result.topLeft.asSolidOrQuadtreeRecursive(maxDepth - 1, leafWidth, leafHeight)
        val topRight = result.topRight.asSolidOrQuadtreeRecursive(maxDepth - 1, leafWidth, leafHeight)
        val bottomLeft = result.bottomLeft.asSolidOrQuadtreeRecursive(maxDepth - 1, leafWidth, leafHeight)
        val bottomRight = result.bottomRight.asSolidOrQuadtreeRecursive(maxDepth - 1,leafWidth, leafHeight)
        return QuadtreeImageNode(
                    width, height,
                    topLeft = topLeft,
                    topRight = topRight,
                    bottomLeft = bottomLeft,
                    bottomRight = bottomRight, name, scope, retryer, stats)
    }

    open suspend fun pixelReader(): PixelReader = ImageNodePixelReader(this::unpacked)

    abstract suspend fun unpacked(): Image
    open suspend fun repaint(
        newPaint: Paint? = null,
        alpha: Double = 1.0,
        name: String,
        retryer: Retryer,
        packer: ImagePacker
    ): ImageNode {
        val unpacked = unpacked()
        val view = ImageView(unpacked)
        if (newPaint != null) {
            val colorLayer = ColorInput(0.0, 0.0, width.toDouble(), height.toDouble(), newPaint)
            val blend = Blend()
            blend.mode = BlendMode.SRC_ATOP
            blend.topInput = colorLayer
            blend.bottomInput = null
            view.effect = blend
        }
        view.opacity = alpha
        view.cacheHint = CacheHint.QUALITY
        view.isSmooth = true
        return packer.packImage(doJfx(name, retryer) { view.snapshot(DEFAULT_SNAPSHOT_PARAMS, null) }, null, name)
    }

    suspend fun asPng(): ByteArray = packingTask.await()
    suspend fun writePng(destination: File) = withContext(Dispatchers.IO) {
        val pngBytes = asPng()
        destination.parentFile?.mkdirs()
        FileOutputStream(destination).use { it.write(pngBytes) }
    }

    abstract override fun toString(): String
}

fun alphaBlend(foreground: Color, background: Color): Color {
    if (foreground.opacity == 1.0) {
        return foreground
    }
    val outOpacity = foreground.opacity + (1 - foreground.opacity) * background.opacity
    if (outOpacity == 0.0) {
        return Color.TRANSPARENT
    }
    val outRed = alphaBlendChannel(foreground, background, outOpacity, Color::getRed)
    val outGreen = alphaBlendChannel(foreground, background, outOpacity, Color::getGreen)
    val outBlue = alphaBlendChannel(foreground, background, outOpacity, Color::getBlue)
    return Color(outRed, outGreen, outBlue, outOpacity)
}

private fun alphaBlendChannel(
    foreground: Color,
    background: Color,
    outOpacity: Double,
    channelSelector: (Color) -> Double
) = (channelSelector(foreground) * foreground.opacity + channelSelector(background) * background.opacity * (1 - foreground.opacity)) / outOpacity

suspend fun superimpose(background: Paint = Color.TRANSPARENT, layers: List<ImageNode>, width: Double, height: Double,
                        name: String, retryer: Retryer, packer: ImagePacker
): ImageNode {
    val (realBackgroundIndex, realBackgroundNode) = layers.withIndex().findLast {
        it.value.let { node -> node is SolidColorImageNode && node.color.isOpaque }
    } ?: IndexedValue(-1, null)
    val visibleLayers = layers.subList(realBackgroundIndex + 1, layers.size)
    var visibleLayerIndex = 0
    var realBackgroundPaint = (realBackgroundNode as SolidColorImageNode?)?.color ?: background
    if (realBackgroundPaint is Color && realBackgroundIndex >= 0) {
        val layersCollapsedIntoBackground = visibleLayers.takeWhile { it is SolidColorImageNode }
        visibleLayerIndex = layersCollapsedIntoBackground.size
        layersCollapsedIntoBackground.forEach {
            realBackgroundPaint = alphaBlend(foreground = (it as SolidColorImageNode).color,
                    background = realBackgroundPaint as Color)
        }
    }
    val layersAfterCollapsing = mutableListOf<ImageNode>()
    while (visibleLayerIndex < visibleLayers.size) {
        val visibleLayer = visibleLayers[visibleLayerIndex]
        val previousLayer = layersAfterCollapsing.lastOrNull()
        if (visibleLayer is SolidColorImageNode && previousLayer is SolidColorImageNode) {
                layersAfterCollapsing[layersAfterCollapsing.size - 1] = SolidColorImageNode(
                    alphaBlend(foreground = visibleLayer.color, background = previousLayer.color),
                    width.toInt(), height.toInt(), name, visibleLayer.scope, retryer, visibleLayer.stats)
        } else {
            layersAfterCollapsing.add(visibleLayer)
        }
        visibleLayerIndex++
    }
    if (layersAfterCollapsing.all { it is QuadtreeImageNode || it is SolidColorImageNode }
            && layersAfterCollapsing.any {it is QuadtreeImageNode}
            && height > packer.leafSize) {
        val tileWidth = width / 2
        val tileHeight = height / 2
        val quadtreeLayers = layersAfterCollapsing.map {it.asQuadtree()}
        return QuadtreeImageNode(
            width = width.toInt(),
            height = height.toInt(),
            topLeft = superimpose(realBackgroundPaint, quadtreeLayers.map{it.topLeft},
                tileWidth, tileHeight, name, retryer, packer),
            topRight = superimpose(realBackgroundPaint, quadtreeLayers.map{it.topRight},
                tileWidth, tileHeight, name, retryer, packer),
            bottomLeft = superimpose(realBackgroundPaint, quadtreeLayers.map{it.bottomLeft},
                tileWidth, tileHeight, name, retryer, packer),
            bottomRight = superimpose(realBackgroundPaint, quadtreeLayers.map{it.bottomRight},
                tileWidth, tileHeight, name, retryer, packer),
            name, layersAfterCollapsing[0].scope, retryer, layersAfterCollapsing[0].stats
        )

    }
    val canvas = Canvas(width, height)
    val canvasCtx = canvas.graphicsContext2D
    if (realBackgroundPaint != Color.TRANSPARENT) {
        doJfx(name, retryer) {
            canvasCtx.fill = realBackgroundPaint
            canvasCtx.fillRect(0.0, 0.0, width, height)
        }
    }
    val layerImagesAfterCollapsing = layersAfterCollapsing.map {it.unpacked()}
    layerImagesAfterCollapsing.forEach { doJfx(name, retryer) { canvasCtx.drawImage(it, 0.0, 0.0) } }
    return packer.packImage(doJfx(name, retryer) { canvas.snapshot(DEFAULT_SNAPSHOT_PARAMS, null) }, null, name)
}