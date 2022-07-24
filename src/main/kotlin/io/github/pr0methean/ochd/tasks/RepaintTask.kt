package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.DEFAULT_SNAPSHOT_PARAMS
import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.Retryer
import io.github.pr0methean.ochd.packedimage.ImageNode
import io.github.pr0methean.ochd.packedimage.ImagePacker
import javafx.scene.canvas.Canvas
import javafx.scene.effect.Blend
import javafx.scene.effect.BlendMode
import javafx.scene.effect.ColorInput
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

    override suspend fun createImage(): ImageNode {
        if (paint == null && alpha == 1.0) {
            return base.getImage()
        }
        val unpacked = base.getImage().unpacked()
        val blend = if (paint != null) {
            val colorLayer = ColorInput(0.0, 0.0, unpacked.width, unpacked.height, paint)
            Blend().also {
                it.mode = BlendMode.SRC_ATOP
                it.topInput = colorLayer
                it.bottomInput = null
            }
        } else null
        val snapshot = doJfx(name, retryer) {
            val canvas = Canvas(unpacked.width, unpacked.height)
            val gfx = canvas.graphicsContext2D
            canvas.opacity = alpha
            blend?.let { gfx.setEffect(it) }
            gfx.drawImage(unpacked, 0.0, 0.0)
            canvas.snapshot(DEFAULT_SNAPSHOT_PARAMS, null)
        }
        return packer.packImage(snapshot, null, name)
    }

    override fun formatTo(buffer: StringBuilder) {
        buffer.append("RepaintTask(").append(base).append(',').append(paint).append(',')
            .append(Unbox.box(alpha)).append(")")
    }
}