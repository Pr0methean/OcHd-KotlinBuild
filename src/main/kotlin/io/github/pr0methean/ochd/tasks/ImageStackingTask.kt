package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.DEFAULT_SNAPSHOT_PARAMS
import io.github.pr0methean.ochd.ImageProcessingContext
import io.github.pr0methean.ochd.LayerList
import io.github.pr0methean.ochd.packedimage.PackedImage
import javafx.scene.canvas.Canvas
import javafx.scene.image.Image
import javafx.scene.paint.Color
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.sync.withPermit

data class ImageStackingTask(
    val layers: LayerList,
    override val ctx: ImageProcessingContext
): TextureTask(ctx) {
    override suspend fun runTask(): PackedImage =
        ctx.multipleSubtaskSemaphore.withPermit { super.runTask() }

    val size = ctx.tileSize
    override suspend fun computeImage(): Image {
        val canvas = Canvas(size.toDouble(), size.toDouble())
        val canvasCtx = canvas.graphicsContext2D
        val layerImages = layers.layers.asFlow().map(TextureTask::getImage).toList()
        return doJfx {
            if (layers.background != Color.TRANSPARENT) {
                canvasCtx.fill = layers.background
                canvasCtx.fillRect(0.0, 0.0, canvas.width, canvas.height)
            }
            layerImages.forEach {canvasCtx.drawImage(it.unpacked(), 0.0, 0.0)}
            return@doJfx canvas.snapshot(DEFAULT_SNAPSHOT_PARAMS, null)
        }
    }
}