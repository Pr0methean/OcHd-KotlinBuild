package io.github.pr0methean.ochd.packedimage

import io.github.pr0methean.ochd.ImageProcessingContext
import javafx.application.Platform
import javafx.embed.swing.SwingFXUtils
import javafx.scene.image.Image
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

@Suppress("unused")
internal abstract class ImageNodeTest(val expected: Image, val actual: suspend () -> ImageNode) {

    val className = this::class.simpleName

    companion object {
        @JvmStatic @BeforeAll fun setUpAll() {
            try {
                Platform.startup {}
            } catch (ignored: java.lang.IllegalStateException) {}
        }
    }

    suspend fun unpacked() {
        assertImagesEqual("$className.unpacked", expected, actual().unpacked())
    }


    abstract suspend fun repaint()

    @Test
    fun testUnpacked() = runBlocking { unpacked() }
    @Test
    fun testRepaint() = runBlocking { repaint() }

}