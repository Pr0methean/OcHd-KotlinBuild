package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.DEFAULT_SNAPSHOT_PARAMS
import io.github.pr0methean.ochd.ImageProcessingContext
import io.github.pr0methean.ochd.LayerList
import io.github.pr0methean.ochd.packedimage.PackedImage
import javafx.scene.canvas.Canvas
import javafx.scene.image.Image
import javafx.scene.image.WritableImage
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.withIndex

data class AnimationColumnTask(
    private val frames: LayerList,
    override val ctx: ImageProcessingContext
): TextureTask(ctx) {
    override suspend fun computeImage(): Image {
        val size = ctx.tileSize
        val height = size * frames.layers.size
        val canvas = Canvas(size.toDouble(), height.toDouble())
        val canvasCtx = canvas.graphicsContext2D
        ctx.decorateFlow(frames.layers.asFlow()
                .map(TextureTask::await)
                .map(PackedImage::unpacked)
                .withIndex())
                .collect {
            doJfx {canvasCtx.drawImage(it.value, 0.0, (size * it.index).toDouble())}
        }
        val out = WritableImage(size, height)
        doJfx {canvas.snapshot(DEFAULT_SNAPSHOT_PARAMS, out)}
        return out
    }
}
