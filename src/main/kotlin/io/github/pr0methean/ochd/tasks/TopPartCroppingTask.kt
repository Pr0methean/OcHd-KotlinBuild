package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.MEMORY_INTENSE_COROUTINE_CONTEXT
import io.github.pr0methean.ochd.Retryer
import io.github.pr0methean.ochd.packedimage.ImagePacker
import javafx.scene.image.Image
import javafx.scene.image.WritableImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext

const val TOP_PORTION = 11.0/32
data class TopPartCroppingTask(
    val base: TextureTask,
    val width: Int,
    override val packer: ImagePacker,
    override val scope: CoroutineScope,
    override val stats: ImageProcessingStats,
    override val retryer: Retryer
): UnpackingTextureTask(packer, scope, stats, retryer) {
    private val height = (width * TOP_PORTION).toInt()
    override suspend fun computeImage(): Image = withContext(MEMORY_INTENSE_COROUTINE_CONTEXT) {
        val pixelReader = base.getImage().unpacked().pixelReader
        doJfx(name) {
            return@doJfx WritableImage(pixelReader, width, height)
        }
    }

    override fun dependencies(): Collection<Task> = listOf(base)

    override fun formatTo(buffer: StringBuilder) {
        buffer.append("Top part of ")
        base.formatTo(buffer)
    }
}