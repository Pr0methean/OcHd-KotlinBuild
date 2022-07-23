package io.github.pr0methean.ochd.packedimage

import io.github.pr0methean.ochd.DEFAULT_SNAPSHOT_PARAMS
import io.github.pr0methean.ochd.ImageProcessingContext
import io.github.pr0methean.ochd.tasks.doJfx
import javafx.application.Platform
import javafx.embed.swing.SwingFXUtils
import javafx.scene.canvas.Canvas
import javafx.scene.image.Image
import javafx.scene.image.WritableImage
import javafx.scene.paint.Color
import kotlinx.coroutines.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.awt.image.RenderedImage
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.test.fail
import io.github.pr0methean.ochd.packedimage.TEST_IMAGE_PROCESSING_CONTEXT as ctx

val TEST_TILE_SIZE = 1024
val TEST_IMAGE_PROCESSING_CONTEXT = ImageProcessingContext(
    name = "TestImageProcessingContext",
    tileSize = TEST_TILE_SIZE,
    scope = CoroutineScope(Dispatchers.Default),
    svgDirectory = Paths.get("svg").toAbsolutePath().toFile(),
    outTextureRoot = Files.createTempDirectory("ImageNodeTest").toFile()
)

suspend fun assertImagesEqual(testName: String, expected: Image, actual: Image) {
    assertEquals(expected.height, actual.height)
    assertEquals(expected.width, actual.width)
    val expectedReader = expected.pixelReader
    val actualReader = actual.pixelReader
    val totalPixels = expected.height.toInt() * expected.width.toInt()
    var mismatchedPixels = 0
    for (y in 0 until expected.height.toInt()) {
        for (x in 0 until expected.width.toInt()) {
            val expectedPixel = expectedReader.getArgb(x, y)
            val actualPixel = actualReader.getArgb(x, y)
            if(expectedPixel != actualPixel) {
                val expectedColor = expectedReader.getColor(x, y)
                val actualColor = actualReader.getColor(x, y)
                if (abs(expectedColor.red - actualColor.red) >= 0.05
                    || abs(expectedColor.green - actualColor.green) >= 0.05
                    || abs(expectedColor.blue - actualColor.blue) >= 0.05
                    || abs(expectedColor.opacity - actualColor.opacity) >= 0.05
                ) {
                    mismatchedPixels++
                }
            }
        }
    }
    if (mismatchedPixels > totalPixels / 100) {
        withContext(Dispatchers.IO) {
            ImageIO.write(
                SwingFXUtils.fromFXImage(expected, null) as RenderedImage,
                "PNG",
                File("expected-$testName.png")
            )
            ImageIO.write(
                SwingFXUtils.fromFXImage(actual, null) as RenderedImage,
                "PNG",
                File("actual-$testName.png")
            )
        }
        fail("Too many mismatched pixels")
    }
}

fun <T> runBlocking(block: suspend CoroutineScope.() -> T)
        = runBlocking(ctx.scope.coroutineContext, block = block)

internal abstract class ImageNodeTest(val expected: Image, val actual: suspend () -> ImageNode) {

    val className = this::class.simpleName

    companion object {
        @JvmStatic @BeforeAll fun setUpAll() {
            try {
                Platform.startup {}
            } catch (ignored: java.lang.IllegalStateException) {}
        }
    }

    abstract suspend fun isSolidColor()

    abstract suspend fun toSolidColorIfPossible()

    suspend fun asQuadtree() {
        assertImagesEqual("$className.asQuadtree", expected, actual().asQuadtree().unpacked())
    }

    suspend fun asSolidOrQuadtreeRecursive() {
        assertImagesEqual("$className.asSolidOrQuadtreeRecursive", expected, actual().asSolidOrQuadtreeRecursive(5, 4, 4).unpacked())
    }

    suspend fun pixelReader() {
        val actualNode = actual()
        val actualImage = WritableImage(actualNode.width, actualNode.height)
        actualImage.pixelWriter.setPixels(0, 0, actualNode.width, actualNode.height, actualNode.pixelReader(), 0, 0)
        assertImagesEqual("$className.pixelReader", expected, actualImage)
    }

    suspend fun unpacked() {
        assertImagesEqual("$className.unpacked", expected, actual().unpacked())
    }

    suspend fun superimposeOnSemitransparentSolidColor() {
        val background = Color(1.0, 0.75, 0.5, 0.5)
        val canvas = Canvas(TEST_TILE_SIZE.toDouble(), TEST_TILE_SIZE.toDouble())
        val graphics = canvas.graphicsContext2D
        graphics.fill = background
        var expectedSuperimpose: Image? = null
        doJfx("superimpose", ctx.retryer) {
            graphics.fillRect(0.0, 0.0, TEST_TILE_SIZE.toDouble(), TEST_TILE_SIZE.toDouble())
            graphics.drawImage(expected, 0.0, 0.0)
            expectedSuperimpose = canvas.snapshot(DEFAULT_SNAPSHOT_PARAMS, null)
        }
        val actualSuperimpose = superimpose(background, listOf(actual()),
            TEST_TILE_SIZE.toDouble(), TEST_TILE_SIZE.toDouble(), "superimpose",
            ctx.retryer, ctx.packer).unpacked()
        assertImagesEqual("$className.superimposeOn", expectedSuperimpose!!, actualSuperimpose)
    }

    suspend fun superimposeBelowSemitransparentSolidColor() {
        val foreground = Color(1.0, 0.75, 0.5, 0.5)
        val canvas = Canvas(TEST_TILE_SIZE.toDouble(), TEST_TILE_SIZE.toDouble())
        val graphics = canvas.graphicsContext2D
        graphics.fill = foreground
        var expectedSuperimpose: Image? = null
        doJfx("superimpose", ctx.retryer) {
            graphics.drawImage(expected, 0.0, 0.0)
            graphics.fillRect(0.0, 0.0, TEST_TILE_SIZE.toDouble(), TEST_TILE_SIZE.toDouble())
            expectedSuperimpose = canvas.snapshot(DEFAULT_SNAPSHOT_PARAMS, null)
        }
        val actualSuperimpose = superimpose(Color.TRANSPARENT, listOf(actual(),
                SolidColorImageNode(foreground, TEST_TILE_SIZE, TEST_TILE_SIZE, "foreground", ctx.scope, ctx.retryer, ctx.stats)),
            TEST_TILE_SIZE.toDouble(), TEST_TILE_SIZE.toDouble(), "superimpose",
            ctx.retryer, ctx.packer).unpacked()
        assertImagesEqual("$className.superimposeBelow", expectedSuperimpose!!, actualSuperimpose)
    }

    abstract suspend fun repaint()

    @Test
    fun testIsSolidColor() = runBlocking { isSolidColor() }
    @Test
    fun testToSolidColorIfPossible() = runBlocking { toSolidColorIfPossible() }
    @Test
    fun testAsQuadtree() = runBlocking { asQuadtree() }
    @Test
    fun testAsSolidOrQuadtreeRecursive() = runBlocking { asSolidOrQuadtreeRecursive() }
    @Test
    fun testPixelReader() = runBlocking { pixelReader() }
    @Test
    fun testUnpacked() = runBlocking { unpacked() }
    @Test
    fun testRepaint() = runBlocking { repaint() }
    @Test
    fun testSuperimposeOnSemitransparentColor() = runBlocking { superimposeOnSemitransparentSolidColor() }
    @Test
    fun testSuperimposeUnderSemitransparentColor() = runBlocking { superimposeBelowSemitransparentSolidColor() }
}