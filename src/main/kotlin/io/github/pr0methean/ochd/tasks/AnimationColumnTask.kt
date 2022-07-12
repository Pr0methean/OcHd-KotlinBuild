package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.DEFAULT_SNAPSHOT_PARAMS
import io.github.pr0methean.ochd.ImageProcessingContext
import io.github.pr0methean.ochd.packedimage.PackedImage
import javafx.scene.canvas.Canvas
import javafx.scene.image.Image
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.withIndex
import kotlinx.coroutines.runBlocking

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

    override fun willExpandHeap(): Boolean = super.willExpandHeap() || frames.any {
            it.willExpandHeap() || it.isComplete() && !runBlocking {it.getImage()}.isAlreadyUnpacked() }
}
