package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.*
import io.github.pr0methean.ochd.packedimage.ImagePacker
import io.github.pr0methean.ochd.packedimage.PackedImage
import javafx.scene.canvas.Canvas
import javafx.scene.image.Image
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.withIndex

data class AnimationColumnTask(
    val frames: List<Deferred<PackedImage>>, val width: Int,
    override val packer: ImagePacker, override val scope: CoroutineScope,
    override val stats: ImageProcessingStats,
    override val retryer: Retryer
): AbstractTextureTask(packer, scope, stats, retryer) {
    @Suppress("OVERRIDE_BY_INLINE")
    override suspend inline fun computeImage(): Image {
        val height = width * frames.size
        val canvas = Canvas(width.toDouble(), height.toDouble())
        val canvasCtx = canvas.graphicsContext2D
        frames.asFlow().map(Deferred<PackedImage>::await).map(PackedImage::unpacked)
                .withIndex()
                .collect {
                    doJfx(toString(), retryer) { canvasCtx.drawImage(it.value, 0.0, (width * it.index).toDouble()) }
                }
        return doJfx(toString(), retryer) { canvas.snapshot(DEFAULT_SNAPSHOT_PARAMS, null) }
    }

    override fun formatTo(buffer: StringBuilder) {
        buffer.append("AnimationColumnTask for ").appendList(frames)
    }
}
