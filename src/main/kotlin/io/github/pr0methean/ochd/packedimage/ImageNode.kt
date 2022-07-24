package io.github.pr0methean.ochd.packedimage

import io.github.pr0methean.ochd.*
import io.github.pr0methean.ochd.packedimage.QuadtreeImageNode.Quadrant
import io.github.pr0methean.ochd.tasks.doJfx
import javafx.embed.swing.SwingFXUtils
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.effect.Blend
import javafx.scene.effect.BlendMode
import javafx.scene.effect.ColorInput
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.image.PixelReader
import javafx.scene.image.WritableImage
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.logging.log4j.LogManager
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.*
import javax.imageio.ImageIO

private val logger = LogManager.getLogger("ImageNode")
abstract class ImageNode(
    val width: Int, val height: Int, initialUnpacked: Image? = null, initialPacked: ByteArray? = null,
    val name: String, val scope: CoroutineScope, val retryer: Retryer,
    val stats: ImageProcessingStats,
    val packer: ImagePacker
) {

    protected val pngBytes = StrongAsyncLazy(initialPacked) {
        ByteArrayOutputStream().use {
            retryer.retrying<ByteArray>("Compression of a ${width}×$height chunk of $name") {
                if (height >= MIN_LOGGABLE_SIZE) {
                    stats.onCompressPngImage("a ${width}×$height chunk of $name")
                }
                @Suppress("BlockingMethodInNonBlockingContext")
                ImageIO.write(SwingFXUtils.fromFXImage(unpacked(), null), "PNG", it)
                return@retrying it.toByteArray()
            }
        }.also {
            if (height >= MIN_LOGGABLE_SIZE) {
                logger.info("Done compressing a {}×{} chunk of {}", width, height, name)
            }
        }
    }

    protected open val isSolidColor = StrongAsyncLazy {
        val pixelReader = pixelReader()
        val topLeftArgb = pixelReader.getArgb(0, 0)
        for (y in 0 until height) {
            for (x in 0 until width) {
                if (pixelReader.getArgb(x, y) != topLeftArgb) {
                    return@StrongAsyncLazy false
                }
            }
        }
        return@StrongAsyncLazy true
    }

    open suspend fun renderTo(out: GraphicsContext, x: Int, y: Int) {
        val unpacked = unpacked()
        doJfx(name, retryer) {out.drawImage(unpacked, x.toDouble(), y.toDouble())}
    }

    open suspend fun isSolidColor() = isSolidColor.get()

    open suspend fun toSolidColorIfPossible(): ImageNode {
        return if (isSolidColor()) {
            SolidColorImageNode(unpacked.getNow(), pixelReader().getColor(0, 0), width, height, name, scope, retryer, stats, packer)
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
                BitmapImageNode(quadrantBuffer, null, name, scope, retryer, stats, tileWidth, tileHeight, packer)
        }
        return QuadtreeImageNode(
            unpacked.getNow(), width, height,
            topLeft = quadrants[Quadrant.UPPER_LEFT]!!.toSolidColorIfPossible(),
            topRight = quadrants[Quadrant.UPPER_RIGHT]!!.toSolidColorIfPossible(),
            bottomLeft = quadrants[Quadrant.LOWER_LEFT]!!.toSolidColorIfPossible(),
            bottomRight = quadrants[Quadrant.LOWER_RIGHT]!!.toSolidColorIfPossible(),
            name, scope, retryer, stats, packer)
    }

    open suspend fun asSolidOrQuadtreeDeduplicatedRecursive(
        maxDepth: Int,
        leafWidth: Int,
        leafHeight: Int
    ): ImageNode {
        if (width <= leafWidth || height <= leafHeight) {
            return packer.deduplicate(this)
        }
        val solid = toSolidColorIfPossible()
        if (solid is SolidColorImageNode) {
            return packer.deduplicate(solid)
        }
        if (maxDepth == 0) {
            return packer.deduplicate(this)
        }
        if (maxDepth == 1) {
            return packer.deduplicate(asQuadtree())
        }
        val result = asQuadtree()
        val topLeft = result.topLeft.asSolidOrQuadtreeDeduplicatedRecursive(maxDepth - 1, leafWidth, leafHeight)
        val topRight = result.topRight.asSolidOrQuadtreeDeduplicatedRecursive(maxDepth - 1, leafWidth, leafHeight)
        val bottomLeft = result.bottomLeft.asSolidOrQuadtreeDeduplicatedRecursive(maxDepth - 1, leafWidth, leafHeight)
        val bottomRight = result.bottomRight.asSolidOrQuadtreeDeduplicatedRecursive(maxDepth - 1,leafWidth, leafHeight)
        return packer.deduplicate(QuadtreeImageNode(
            result.unpacked.getNow() ?: unpacked.getNow(), width, height,
            topLeft = topLeft,
            topRight = topRight,
            bottomLeft = bottomLeft,
            bottomRight = bottomRight, name, scope, retryer, stats, packer))
    }

    abstract val pixelReader: AsyncLazy<PixelReader>
    suspend fun pixelReader(): PixelReader = pixelReader.get()

    open val unpacked: AsyncLazy<Image> = SoftAsyncLazy(initialUnpacked, this::unpack)

    suspend fun unpacked(): Image = unpacked.get()

    abstract suspend fun unpack(): Image
    open suspend fun repaint(
        newPaint: Paint? = null,
        alpha: Double = 1.0,
        name: String,
        retryer: Retryer,
        packer: ImagePacker
    ): ImageNode {
        if (newPaint == null && alpha == 1.0) {
            return packer.deduplicate(this)
        }
        val unpacked = unpacked()
        val blend = if (newPaint != null) {
            val colorLayer = ColorInput(0.0, 0.0, width.toDouble(), height.toDouble(), newPaint)
            Blend().also {
                it.mode = BlendMode.SRC_ATOP
                it.topInput = colorLayer
                it.bottomInput = null
            }
        } else null
        val snapshot = doJfx(name, retryer) {
            val view = ImageView(unpacked)
            view.opacity = alpha
            view.isCache = false
            view.isSmooth = true
            blend?.let { view.effect = it }
            view.snapshot(DEFAULT_SNAPSHOT_PARAMS, null)
        }
        return packer.packImage(snapshot, null, name)
    }

    suspend fun asPng(): ByteArray = pngBytes.get()
    suspend fun writePng(destination: File) = withContext(Dispatchers.IO) {
        val pngBytes = asPng()
        destination.parentFile?.mkdirs()
        FileOutputStream(destination).use { it.write(pngBytes) }
    }

    open suspend fun mergeWithDuplicate(other: ImageNode) {
        pngBytes.mergeWithDuplicate(other.pngBytes)
        unpacked.mergeWithDuplicate(other.unpacked)
        pixelReader.mergeWithDuplicate(other.pixelReader)
        isSolidColor.mergeWithDuplicate(other.isSolidColor)
    }

    abstract fun shouldDeduplicate(): Boolean
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
    val collapsedDeduplicatedLayers = mutableListOf<ImageNode>()
    while (visibleLayerIndex < visibleLayers.size) {
        val visibleLayer = visibleLayers[visibleLayerIndex]
        val previousLayer = collapsedDeduplicatedLayers.lastOrNull()
        if (visibleLayer is SolidColorImageNode && previousLayer is SolidColorImageNode) {
                collapsedDeduplicatedLayers[collapsedDeduplicatedLayers.size - 1] = packer.deduplicate(SolidColorImageNode(
                    null, alphaBlend(foreground = visibleLayer.color, background = previousLayer.color),
                    width.toInt(), height.toInt(), name, visibleLayer.scope, retryer, visibleLayer.stats, packer))
        } else {
            collapsedDeduplicatedLayers
                .add(packer.deduplicate(visibleLayer))
        }
        visibleLayerIndex++
    }
    val layersAfterQuadtreeTransform: List<ImageNode>
    if (height <= packer.leafSize || width <= packer.leafSize || collapsedDeduplicatedLayers.size <= 1) {
        layersAfterQuadtreeTransform = collapsedDeduplicatedLayers
    } else {
        val quadtreeLayers = collapsedDeduplicatedLayers.map {
            if (it is QuadtreeImageNode) {
                it
            } else {
                packer.deduplicate(it.asQuadtree())
            }
        }
        // Convert stack of quadtrees to quadtree of stacks
        layersAfterQuadtreeTransform = listOf(
            packer.deduplicate(
                QuadtreeImageNode(
                    initialUnpacked = collapsedDeduplicatedLayers.singleOrNull()?.unpacked?.getNow(),
                    width = width.toInt(),
                    height = height.toInt(),
                    topLeft = superimpose(
                        realBackgroundPaint, quadtreeLayers.map { it.topLeft },
                        width / 2, height / 2, name, retryer, packer
                    ),
                    topRight = superimpose(
                        realBackgroundPaint, quadtreeLayers.map { it.topRight },
                        width / 2, height / 2, name, retryer, packer
                    ),
                    bottomLeft = superimpose(
                        realBackgroundPaint, quadtreeLayers.map { it.bottomLeft },
                        width / 2, height / 2, name, retryer, packer
                    ),
                    bottomRight = superimpose(
                        realBackgroundPaint, quadtreeLayers.map { it.bottomRight },
                        width / 2, height / 2, name, retryer, packer
                    ),
                    name, collapsedDeduplicatedLayers[0].scope, retryer, collapsedDeduplicatedLayers[0].stats, packer
                )
            )
        )
    }
    if (realBackgroundPaint == Color.TRANSPARENT && layersAfterQuadtreeTransform.size == 1) {
        return layersAfterQuadtreeTransform[0]
    }
    val canvas = Canvas(width, height)
    val canvasCtx = canvas.graphicsContext2D
    if (realBackgroundPaint != Color.TRANSPARENT) {
        doJfx(name, retryer) {
            canvasCtx.fill = realBackgroundPaint
            canvasCtx.fillRect(0.0, 0.0, width, height)
        }
    }
    layersAfterQuadtreeTransform.forEach { it.renderTo(canvasCtx, 0, 0) }
    return packer.packImage(doJfx(name, retryer) { canvas.snapshot(DEFAULT_SNAPSHOT_PARAMS, null) }, null, name)
}