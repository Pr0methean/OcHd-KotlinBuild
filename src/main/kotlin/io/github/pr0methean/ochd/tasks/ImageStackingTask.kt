package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.*
import io.github.pr0methean.ochd.packedimage.ImagePacker
import io.github.pr0methean.ochd.packedimage.PackedImage
import javafx.scene.canvas.Canvas
import javafx.scene.image.Image
import javafx.scene.paint.Color
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.*

data class ImageStackingTask(
    val layers: LayerList,
    val size: Int,
    override val packer: ImagePacker,
    override val scope: CoroutineScope,
    override val stats: ImageProcessingStats,
    override val retryer: Retryer
): AbstractTextureTask(packer, scope, stats, retryer) {

    @Suppress("OVERRIDE_BY_INLINE")
    override suspend inline fun computeImage(): Image {
        val canvas = Canvas(size.toDouble(), size.toDouble())
        val canvasCtx = canvas.graphicsContext2D
        if (layers.background != Color.TRANSPARENT) {
            doJfx("Background for $name", retryer) {
                canvasCtx.fill = layers.background
                canvasCtx.fillRect(0.0, 0.0, canvas.width, canvas.height)
            }
        }
        layers.layers.asFlow().map(Deferred<PackedImage>::await).map(PackedImage::unpacked).withIndex().collect {
            doJfx("Layer ${it.index} for $name", retryer) {
                canvasCtx.drawImage(it.value, 0.0, 0.0)
            }
        }
        return doJfx(name, retryer) {
            canvas.snapshot(DEFAULT_SNAPSHOT_PARAMS, null)
        }
    }

    override fun formatTo(buffer: StringBuilder) {
        buffer.append("Stack: ")
        layers.formatTo(buffer)
    }
}