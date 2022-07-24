package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.*
import io.github.pr0methean.ochd.packedimage.ImageNode
import io.github.pr0methean.ochd.packedimage.ImagePacker
import javafx.scene.canvas.Canvas
import javafx.scene.paint.Color
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList

data class ImageStackingTask(
    val layers: LayerList,
    val size: Int,
    val packer: ImagePacker,
    override val scope: CoroutineScope,
    override val stats: ImageProcessingStats,
    val retryer: Retryer,
    val pool: WritableImagePool
): AbstractTextureTask(scope, stats) {
    constructor(layers: LayerList, size: Int, packer: ImagePacker,
                scope: CoroutineScope,
                stats: ImageProcessingStats,
                retryer: Retryer,
                poolProvider: WritableImagePoolProvider): this(layers, size, packer, scope, stats, retryer,
    poolProvider.getPool(size, size))

    override suspend fun createImage(): ImageNode {
        val width = size.toDouble()
        val height = size.toDouble()
        val name = layers.toString()
        val layerImages = layers.layers.asFlow().map { it.getImage() }.toList()
        val canvas = Canvas(width, height)
        canvas.isCache = true
        val canvasCtx = canvas.graphicsContext2D
        if (layers.background != Color.TRANSPARENT) {
            doJfx(name, retryer) {
                canvasCtx.fill = layers.background
                canvasCtx.fillRect(0.0, 0.0, width, height)
            }
        }
        layerImages.forEach { it.renderTo(canvasCtx, 0, 0) }
        return packer.packImage(pool.borrow {doJfx(name, retryer) {canvas.snapshot(DEFAULT_SNAPSHOT_PARAMS, it)}}, null, name)
    }

    override fun formatTo(buffer: StringBuilder) {
        layers.formatTo(buffer)
    }
}