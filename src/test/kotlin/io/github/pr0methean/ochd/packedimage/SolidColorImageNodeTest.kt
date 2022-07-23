package io.github.pr0methean.ochd.packedimage

import io.github.pr0methean.ochd.DEFAULT_SNAPSHOT_PARAMS
import io.github.pr0methean.ochd.tasks.doJfx
import javafx.scene.canvas.Canvas
import javafx.scene.paint.Color
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import io.github.pr0methean.ochd.packedimage.TEST_IMAGE_PROCESSING_CONTEXT as ctx

internal class SolidColorImageNodeTest : ImageNodeTest(
    expected = createSolidColorImage(Color.ORANGE),
    actual = {SolidColorImageNode(Color.ORANGE, ctx.tileSize, ctx.tileSize, "Solid Orange Test Node", ctx.scope, ctx.retryer, ctx.stats)}) {
    override suspend fun isSolidColor() {
        assertTrue(actual().isSolidColor)
    }

    override suspend fun toSolidColorIfPossible() {
        val actual = actual()
        assertSame(actual, actual.toSolidColorIfPossible())
    }

    override suspend fun repaint() {
        val expected = createSolidColorImage(Color.TEAL)
        val actual = actual().repaint(Color. TEAL, 1.0, "Solid Teal Test Node",
            ctx.retryer, ctx.packer)
        assertImagesEqual("$className.repaint", expected, actual.unpacked())
        assertTrue(actual is SolidColorImageNode)
    }

    @Test
    fun repaintTransparent() = runBlocking {
        val before = SolidColorImageNode(Color.TRANSPARENT, ctx.tileSize, ctx.tileSize, "Transparent Test Node", ctx.scope, ctx.retryer, ctx.stats)
        val after = before.repaint(Color.BLUE, 1.0, "Transparent Test Node 2", ctx.retryer, ctx.packer)
        assertTrue(after is SolidColorImageNode)
        assertEquals(Color.TRANSPARENT, (after as SolidColorImageNode).color)
    }

}

private fun createSolidColorImage(color: Color?) = runBlocking {
    val expectedImage = Canvas(
        ctx.tileSize.toDouble(),
        ctx.tileSize.toDouble()
    )
    return@runBlocking doJfx(
        "create expected image for SolidColorImageNodeTest",
        ctx.retryer
    ) {
        val gfx = expectedImage.graphicsContext2D
        gfx.fill = color
        gfx.fillRect(
            0.0,
            0.0,
            ctx.tileSize.toDouble(),
            ctx.tileSize.toDouble()
        )
        return@doJfx expectedImage.snapshot(DEFAULT_SNAPSHOT_PARAMS, null)
    }
}