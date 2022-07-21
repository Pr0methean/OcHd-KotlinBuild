package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.Retryer
import io.github.pr0methean.ochd.packedimage.ImagePacker
import io.github.pr0methean.ochd.packedimage.PackedImage
import javafx.scene.paint.Paint
import kotlinx.coroutines.CoroutineScope
import org.apache.logging.log4j.util.Unbox

data class RepaintTask(
    val paint: Paint?,
    val base: TextureTask,
    private val size: Int,
    val alpha: Double = 1.0,
    val packer: ImagePacker,
    override val scope: CoroutineScope,
    override val stats: ImageProcessingStats,
    val retryer: Retryer,
) : AbstractTextureTask(scope, stats) {

    override suspend fun createImage(): PackedImage
        = base.getImage().repaint(paint, alpha, base.toString(), retryer, packer)

    override fun formatTo(buffer: StringBuilder) {
        buffer.append("RepaintTask(").append(base).append(',').append(paint).append(',')
            .append(Unbox.box(alpha)).append(")")
    }
}