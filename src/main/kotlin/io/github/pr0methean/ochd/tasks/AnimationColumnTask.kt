package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.*
import io.github.pr0methean.ochd.packedimage.ImagePacker
import javafx.scene.canvas.Canvas
import javafx.scene.image.Image
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.withIndex

data class AnimationColumnTask(
    private val frames: List<TextureTask>, val width: Int,
    override val packer: ImagePacker, override val scope: CoroutineScope, override val stats: ImageProcessingStats,
    override val retryer: Retryer,
    val pool: WritableImagePool
): UnpackingTextureTask(packer, scope, stats, retryer) {
    constructor(frames: List<TextureTask>, width: Int, packer: ImagePacker, scope: CoroutineScope,
                stats: ImageProcessingStats, retryer: Retryer, poolProvider: WritableImagePoolProvider):
            this(frames, width, packer, scope, stats, retryer, poolProvider.getPool(width, width * frames.size))

    private val height = width * frames.size
    override suspend fun computeImage(): Image {
        val frameImages = frames.asFlow()
            .map(TextureTask::getImage)
            .withIndex()
            .toList()
        val canvas = Canvas(width.toDouble(), height.toDouble())
        canvas.isCache = true
        val canvasCtx = canvas.graphicsContext2D
        frameImages.forEach {it.value.renderTo(canvasCtx, 0, height * it.index)}
        return pool.borrow {return@borrow doJfx {return@doJfx canvas.snapshot(DEFAULT_SNAPSHOT_PARAMS, it)}}
    }

    override fun formatTo(buffer: StringBuilder) {
        buffer.append("Animation [").appendList(frames, "; ").append(']')
    }
}
