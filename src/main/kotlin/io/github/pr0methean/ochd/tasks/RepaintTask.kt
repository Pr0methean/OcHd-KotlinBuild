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
import javafx.scene.image.Image
import javafx.scene.image.WritableImage
import javafx.scene.paint.Paint
import kotlinx.coroutines.CoroutineScope
import org.apache.logging.log4j.util.Unbox

data class RepaintTask(
    val paint: Paint?,
    val base: TextureTask,
    val alpha: Double = 1.0,
    override val packer: ImagePacker,
    override val scope: CoroutineScope,
    override val stats: ImageProcessingStats,
    override val retryer: Retryer,
) : UnpackingTextureTask(packer, scope, stats, retryer) {

    override suspend fun createImage(): ImageNode {
        if (paint == null && alpha == 1.0) {
            return base.getImage()
        }
        return super.createImage()
    }

    override suspend fun computeImage(): Image {
        val unpacked = base.getImage().unpacked()
        return retryer.retrying(name) {
            val output = WritableImage(unpacked.width.toInt(), unpacked.height.toInt())
            return@retrying doJfx(name) {
                val canvas = Canvas(unpacked.width, unpacked.height)
                canvas.isCache = true
                val gfx = canvas.graphicsContext2D
                canvas.opacity = alpha
                if (paint != null) {
                    val colorLayer = ColorInput(0.0, 0.0, unpacked.width, unpacked.height, paint)
                    val blend = Blend()
                    blend.mode = BlendMode.SRC_ATOP
                    blend.topInput = colorLayer
                    blend.bottomInput = null
                    gfx.setEffect(blend)
                }
                gfx.drawImage(unpacked, 0.0, 0.0)
                canvas.snapshot(DEFAULT_SNAPSHOT_PARAMS, output)
                if (output.isError) {
                    throw output.exception
                }
                return@doJfx output
            }
        }
    }

    override fun formatTo(buffer: StringBuilder) {
        buffer.append("RepaintTask(").append(base).append(',').append(paint).append(',')
            .append(Unbox.box(alpha)).append(")")
    }
}