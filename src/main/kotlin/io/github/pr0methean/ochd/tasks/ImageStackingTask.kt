package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.*
import io.github.pr0methean.ochd.packedimage.ImageNode
import io.github.pr0methean.ochd.packedimage.ImagePacker
import javafx.scene.canvas.Canvas
import javafx.scene.image.Image
import javafx.scene.image.WritableImage
import javafx.scene.paint.Color
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext

data class ImageStackingTask(
    val layers: LayerList,
    val size: Int,
    override val packer: ImagePacker,
    override val scope: CoroutineScope,
    override val stats: ImageProcessingStats,
    override val retryer: Retryer
): UnpackingTextureTask(packer, scope, stats, retryer) {
    val width = size.toDouble()
    val height = size.toDouble()
    override suspend fun computeImage(): Image {
        val name = layers.toString()
        return withContext(MEMORY_INTENSE_COROUTINE_CONTEXT) {
            val layerImages = layers.layers.asFlow().map { it.getImage() }.map(ImageNode::unpacked).toList()
            val output = retryer.retrying("Create WritableImage for $name") { WritableImage(size, size) }
            return@withContext doJfx(name) {
                val canvas = Canvas(width, height)
                canvas.isCache = true
                val canvasCtx = canvas.graphicsContext2D
                if (layers.background != Color.TRANSPARENT) {

                    canvasCtx.fill = layers.background
                    canvasCtx.fillRect(0.0, 0.0, width, height)
                }
                layerImages.forEach { canvasCtx.drawImage(it, 0.0, 0.0) }
                val snapshot = canvas.snapshot(DEFAULT_SNAPSHOT_PARAMS, output)
                if (snapshot.isError) {
                    throw snapshot.exception
                }
                return@doJfx snapshot
            }
        }
    }

    override fun dependencies(): Collection<Task> = layers.layers

    override fun formatTo(buffer: StringBuilder) {
        layers.formatTo(buffer)
    }
}