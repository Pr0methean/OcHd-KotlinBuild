package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.DEFAULT_SNAPSHOT_PARAMS
import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.Retryer
import io.github.pr0methean.ochd.appendList
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

data class AnimationColumnTask(
    private val frames: List<TextureTask>, val width: Int,
    override val packer: ImagePacker, override val scope: CoroutineScope, override val stats: ImageProcessingStats,
    override val retryer: Retryer
): UnpackingTextureTask(packer, scope, stats, retryer) {
    val height = width * frames.size
    override suspend fun computeImage(): Image {
        val frameImages = frames.asFlow()
            .map(TextureTask::getImage)
            .map(ImageNode::unpacked)
            .withIndex()
            .toList()
        val output = WritableImage(width, height)
        return doJfx("snapshot for $name") {
            val canvas = Canvas(width.toDouble(), height.toDouble())
            canvas.isCache = true
            val canvasCtx = canvas.graphicsContext2D
            frameImages.forEach { canvasCtx.drawImage(it.value, 0.0, (height * it.index).toDouble()) }
            canvas.snapshot(DEFAULT_SNAPSHOT_PARAMS, output)
            if (output.isError) {
                throw output.exception
            }
            output
        }
    }

    override fun formatTo(buffer: StringBuilder) {
        buffer.append("Animation [").appendList(frames, "; ").append(']')
    }
}
