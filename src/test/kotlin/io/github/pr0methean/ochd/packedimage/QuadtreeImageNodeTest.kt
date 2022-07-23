package io.github.pr0methean.ochd.packedimage

import io.github.pr0methean.ochd.DEFAULT_SNAPSHOT_PARAMS
import io.github.pr0methean.ochd.tasks.doJfx
import javafx.scene.canvas.Canvas
import javafx.scene.paint.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.File
import io.github.pr0methean.ochd.packedimage.TEST_IMAGE_PROCESSING_CONTEXT as ctx

private val baseImageTask = ctx.layer("furnaceFrontLit")
val quadtreeTestExpected = runBlocking {baseImageTask.getImage().unpacked()}
val quadtreeTestActual: QuadtreeImageNode = runBlocking {baseImageTask.getImage().asQuadtree()}

internal class QuadtreeImageNodeTest : ImageNodeTest(
    expected = quadtreeTestExpected,
    actual = {quadtreeTestActual}) {
    override suspend fun isSolidColor() {
        assertFalse(actual().isSolidColor)
    }

    override suspend fun toSolidColorIfPossible() {
        val actual = actual()
        assertSame(actual.toSolidColorIfPossible(), actual)
    }

    private val triangles by lazy {runBlocking {
        val blackTriangles = ctx.layer("triangles1").getImage()
        withContext(Dispatchers.IO) {blackTriangles.writePng(File("blackTriangles.png"))}
        val blueTriangles = blackTriangles.repaint(Color.BLUE, 1.0, "blueTriangles", ctx.retryer, ctx.packer)
        withContext(Dispatchers.IO) {blueTriangles.writePng(File("blueTriangles.png"))}
        return@runBlocking blueTriangles
    }}

    @Test
    fun testSuperimposeQuadtreeOnQuadtree() = runBlocking {
        val trianglesImg = triangles.unpacked()
        val expected = doJfx("Build expected image for testSuperimposeQuadtreeOnQuadtree", ctx.retryer) {
            val canvas = Canvas(TEST_TILE_SIZE.toDouble(), TEST_TILE_SIZE.toDouble())
            val gfx = canvas.graphicsContext2D
            gfx.drawImage(trianglesImg, 0.0, 0.0)
            gfx.drawImage(quadtreeTestExpected, 0.0, 0.0)
            canvas.snapshot(DEFAULT_SNAPSHOT_PARAMS, null)
        }
        val actual = superimpose(Color.TRANSPARENT, listOf(triangles.asQuadtree(), quadtreeTestActual),
            TEST_TILE_SIZE.toDouble(),
            TEST_TILE_SIZE.toDouble(), "Actual for testSuperimposeQuadtreeOnQuadtree", ctx.retryer, ctx.packer).unpacked()
        assertImagesEqual("testSuperimposeQuadtreeOnQuadtree", expected, actual)
    }

    @Test
    fun testSuperimposeQuadtreeOnQuadtreeRecursive() = runBlocking {
        val triangles = ctx.layer("triangles1").getImage().repaint(Color.BLUE, 1.0,
            "Triangles for testSuperimposeQuadtreeOnQuadtreeRecursive", ctx.retryer, ctx.packer)
        val trianglesImg = triangles.unpacked()
        val expected = doJfx("Build expected image for testSuperimposeQuadtreeOnQuadtreeRecursive", ctx.retryer) {
            val canvas = Canvas(TEST_TILE_SIZE.toDouble(), TEST_TILE_SIZE.toDouble())
            val gfx = canvas.graphicsContext2D
            gfx.drawImage(trianglesImg, 0.0, 0.0)
            gfx.drawImage(quadtreeTestExpected, 0.0, 0.0)
            canvas.snapshot(DEFAULT_SNAPSHOT_PARAMS, null)
        }
        val actual = superimpose(Color.TRANSPARENT, listOf(triangles.asSolidOrQuadtreeRecursive(5, 2, 2),
                quadtreeTestActual.asSolidOrQuadtreeRecursive(5, 2, 2)),
            TEST_TILE_SIZE.toDouble(),
            TEST_TILE_SIZE.toDouble(), "Actual for testSuperimposeQuadtreeOnQuadtreeRecursive", ctx.retryer, ctx.packer).unpacked()
        assertImagesEqual("testSuperimposeQuadtreeOnQuadtreeRecursive", expected, actual)
    }

    override suspend fun repaint() {
        val repaintedFurnace = ctx.layer(baseImageTask, Color.BLUE).getImage()
        val expectedReader = expected.pixelReader
        assertFalse(repaintedFurnace.isSolidColor)
        val actualReader = repaintedFurnace.pixelReader()
        for (y in 0 until repaintedFurnace.height) {
            for (x in 0 until repaintedFurnace.width) {
                val pixelColor = actualReader.getColor(x, y)
                if (pixelColor != Color.TRANSPARENT) {
                    assertEquals(0.0, pixelColor.red)
                    assertEquals(0.0, pixelColor.green)
                    assertEquals(1.0, pixelColor.blue)
                    assertEquals(expectedReader.getColor(x, y).opacity, pixelColor.opacity)
                } else {
                    assertEquals(Color.TRANSPARENT, expectedReader.getColor(x, y))
                }
            }
        }
    }
}