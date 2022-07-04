package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.DEFAULT_SNAPSHOT_PARAMS
import io.github.pr0methean.ochd.ImageProcessingContext
import io.github.pr0methean.ochd.LayerList
import io.github.pr0methean.ochd.packedimage.PackedImage
import javafx.scene.canvas.Canvas
import javafx.scene.image.Image
import javafx.scene.image.WritableImage

data class AnimationColumnTask(
    private val frames: LayerList,
    override val size: Int,
    val ctx: ImageProcessingContext
): ImageCombiningTask(frames, size, ctx) {
    override fun doBlockingJfx(input: List<PackedImage>): Image {
        val canvas = Canvas(size.toDouble(), (size * input.size).toDouble())
        val canvasCtx = canvas.graphicsContext2D
        for ((index, frame) in input.withIndex()) {
            canvasCtx.drawImage(frame.unpack(), 0.0, (size * index).toDouble())
        }
        val out = WritableImage(size, size * input.size)
        retryOnOomBlocking { canvas.snapshot(DEFAULT_SNAPSHOT_PARAMS, out) }
        return out
    }
}
