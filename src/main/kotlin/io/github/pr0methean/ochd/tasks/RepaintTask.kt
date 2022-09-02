package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.DEFAULT_SNAPSHOT_PARAMS
import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.tasks.caching.TaskCache
import io.github.pr0methean.ochd.tasks.caching.noopTaskCache
import javafx.scene.effect.Blend
import javafx.scene.effect.BlendMode
import javafx.scene.effect.ColorInput
import javafx.scene.image.Image
import javafx.scene.image.WritableImage
import javafx.scene.paint.Paint
import java.util.*

class RepaintTask(
    override val base: Task<Image>,
    val paint: Paint?,
    val alpha: Double = 1.0,
    cache: TaskCache<Image>,
    val stats: ImageProcessingStats
): TransformingTask<Image, Image>("$base@$paint@$alpha", base, cache, { baseImage ->
    val name = "$base@$paint@$alpha"
    stats.onTaskLaunched("RepaintTask", name)
    val canvas = createCanvas(baseImage.width, baseImage.height, name)
    val output = WritableImage(baseImage.width.toInt(), baseImage.height.toInt())
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
    gfx.isImageSmoothing = false
    gfx.drawImage(baseImage, 0.0, 0.0)
    val snapshot = doJfx(name) {
        awaitFreeMemory(4 * baseImage.width.toLong() * baseImage.height.toLong(), name)
        canvas.snapshot(DEFAULT_SNAPSHOT_PARAMS, output)
        if (output.isError) {
            throw output.exception
        }
        return@doJfx output
    }
    stats.onTaskCompleted("RepaintTask", "$base@$paint@$alpha")
    snapshot
}), ImageTask {
    override val asPng: Task<ByteArray> by lazy { PngCompressionTask(this, noopTaskCache(), stats) }
    override suspend fun mergeWithDuplicate(other: Task<Image>): ImageTask {
        return super.mergeWithDuplicate(other) as ImageTask
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