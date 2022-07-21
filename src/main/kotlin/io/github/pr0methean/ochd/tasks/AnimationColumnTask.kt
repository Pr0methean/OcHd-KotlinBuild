package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.DEFAULT_SNAPSHOT_PARAMS
import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.Retryer
import io.github.pr0methean.ochd.appendList
import io.github.pr0methean.ochd.packedimage.ImagePacker
import io.github.pr0methean.ochd.packedimage.PackedImage
import javafx.scene.canvas.Canvas
import javafx.scene.image.Image
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.withIndex

data class AnimationColumnTask(
    private val frames: List<TextureTask>, val width: Int,
    override val packer: ImagePacker, override val scope: CoroutineScope, override val stats: ImageProcessingStats,
    override val retryer: Retryer
): UnpackingTextureTask(packer, scope, stats, retryer) {
    override suspend fun computeImage(): Image {
        val height = width * frames.size
        val canvas = Canvas(width.toDouble(), height.toDouble())
        val canvasCtx = canvas.graphicsContext2D
        frames.asFlow()
                .map(TextureTask::getImage)
                .map(PackedImage::unpacked)
                .withIndex()
                .collect {
            doJfx {canvasCtx.drawImage(it.value, 0.0, (width * it.index).toDouble())}
        }
        return doJfx {canvas.snapshot(DEFAULT_SNAPSHOT_PARAMS, null)}
    }

    override fun formatTo(buffer: StringBuilder) {
        buffer.append("Animation [").appendList(frames, "; ").append(']')
    }
}
