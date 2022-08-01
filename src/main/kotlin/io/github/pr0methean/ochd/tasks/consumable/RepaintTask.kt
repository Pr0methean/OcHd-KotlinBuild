package io.github.pr0methean.ochd.tasks.consumable

import io.github.pr0methean.ochd.DEFAULT_SNAPSHOT_PARAMS
import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.tasks.consumable.caching.StrongTaskCache
import io.github.pr0methean.ochd.tasks.consumable.caching.TaskCache
import javafx.scene.canvas.Canvas
import javafx.scene.effect.Blend
import javafx.scene.effect.BlendMode
import javafx.scene.effect.ColorInput
import javafx.scene.image.Image
import javafx.scene.image.WritableImage
import javafx.scene.paint.Paint
import java.util.*

class RepaintTask(
    override val base: ConsumableTask<Image>,
    val paint: Paint?,
    val alpha: Double = 1.0,
    override val cache: TaskCache<Image>,
    val stats: ImageProcessingStats
): SlowTransformingConsumableTask<Image, Image>("$base@$paint@$alpha", base, cache, { baseImage ->
    val output = WritableImage(baseImage.width.toInt(), baseImage.height.toInt())
    doJfx("Repaint $base with $paint") {
        val canvas = Canvas(baseImage.width, baseImage.height)
        canvas.isCache = true
        val gfx = canvas.graphicsContext2D
        canvas.opacity = alpha
        if (paint != null) {
            val colorLayer = ColorInput(0.0, 0.0, baseImage.width, baseImage.height, paint)
            val blend = Blend()
            blend.mode = BlendMode.SRC_ATOP
            blend.topInput = colorLayer
            blend.bottomInput = null
            gfx.setEffect(blend)
        }
        gfx.drawImage(baseImage, 0.0, 0.0)
        canvas.snapshot(DEFAULT_SNAPSHOT_PARAMS, output)
        if (output.isError) {
            throw output.exception
        }
        return@doJfx output
    }
}), ConsumableImageTask {
    override val unpacked: ConsumableTask<Image> = this
    override val asPng: ConsumableTask<ByteArray> = PngCompressionTask(this, StrongTaskCache(), stats)
    override suspend fun checkSanity() {
        super<SlowTransformingConsumableTask>.checkSanity()
        asPng.checkSanity()
    }

    override fun equals(other: Any?): Boolean {
        return (this === other) || (other is RepaintTask
                && other.base == base
                && other.paint == paint
                && other.alpha == alpha)
    }

    override fun hashCode(): Int {
        return Objects.hash(base, paint, alpha)
    }
}