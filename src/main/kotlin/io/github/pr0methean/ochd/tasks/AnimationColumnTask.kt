package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.*
import io.github.pr0methean.ochd.packedimage.ImageNode
import io.github.pr0methean.ochd.packedimage.ImagePacker
import javafx.scene.canvas.Canvas
import javafx.scene.image.Image
import javafx.scene.image.WritableImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.withIndex
import kotlinx.coroutines.sync.Semaphore

data class AnimationColumnTask(
    private val frames: List<TextureTask>, val width: Int,
    override val packer: ImagePacker, override val scope: CoroutineScope, override val stats: ImageProcessingStats,
    override val retryer: Retryer,
    val canvasSemaphore: Semaphore?
): UnpackingTextureTask(packer, scope, stats, retryer) {
    val height = width * frames.size
    override suspend fun computeImage(): Image {
        val frameImages = frames.asFlow()
            .map(TextureTask::getImage)
            .map(ImageNode::unpacked)
            .withIndex()
            .toList()
        val output = WritableImage(width, height)
        canvasSemaphore.withPermitIfNeeded {doJfx("snapshot for $name", retryer) {
            val canvas = Canvas(width.toDouble(), height.toDouble())
            canvas.isCache = true
            val canvasCtx = canvas.graphicsContext2D
            frameImages.forEach { canvasCtx.drawImage(it.value, 0.0, (height * it.index).toDouble()) }
            canvas.snapshot(DEFAULT_SNAPSHOT_PARAMS, output)
            if (output.isError) {
                throw output.exception
            }
        }}
        return output
    }

    override fun formatTo(buffer: StringBuilder) {
        buffer.append("Animation [").appendList(frames, "; ").append(']')
    }
}
