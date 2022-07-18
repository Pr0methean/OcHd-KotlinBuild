package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.DEFAULT_SNAPSHOT_PARAMS
import io.github.pr0methean.ochd.ImageProcessingContext
import io.github.pr0methean.ochd.appendList
import io.github.pr0methean.ochd.packedimage.PackedImage
import javafx.scene.canvas.Canvas
import javafx.scene.image.Image
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.withIndex
import java.lang.StringBuilder

data class AnimationColumnTask(
    private val frames: List<TextureTask>,
    override val ctx: ImageProcessingContext
): AbstractTextureTask(ctx) {
    override suspend fun computeImage(): Image {
        val size = ctx.tileSize
        val height = size * frames.size
        val canvas = Canvas(size.toDouble(), height.toDouble())
        val canvasCtx = canvas.graphicsContext2D
        isAllocated = true
        frames.asFlow()
                .map(TextureTask::getImage)
                .map(PackedImage::unpacked)
                .withIndex()
                .collect {
            doJfx {canvasCtx.drawImage(it.value, 0.0, (size * it.index).toDouble())}
        }
        return doJfx {canvas.snapshot(DEFAULT_SNAPSHOT_PARAMS, null)}
    }

    override fun toString(): String {
        return "Animation [${frames.joinToString("; ")}]"
    }

    override fun formatTo(buffer: StringBuilder) {
        buffer.append("Animation [").appendList(frames, "; ").append(']')
    }

    override fun willExpandHeap(): Boolean = super.willExpandHeap() || frames.any {
            it.willExpandHeap() || it.getImageNow()?.isAlreadyUnpacked() != true }
}
