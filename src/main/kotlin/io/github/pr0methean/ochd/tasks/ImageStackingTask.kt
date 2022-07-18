package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.*
import io.github.pr0methean.ochd.packedimage.ImagePacker
import javafx.scene.canvas.Canvas
import javafx.scene.image.Image
import javafx.scene.paint.Color
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import java.lang.StringBuilder

data class ImageStackingTask(
    val layers: LayerList,
    val size: Int,
    override val packer: ImagePacker,
    override val scope: CoroutineScope,
    override val stats: ImageProcessingStats,
    override val retryer: Retryer
): AbstractTextureTask(packer, scope, stats, retryer) {

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

    override fun formatTo(buffer: StringBuilder) {
        layers.formatTo(buffer)
    }
}