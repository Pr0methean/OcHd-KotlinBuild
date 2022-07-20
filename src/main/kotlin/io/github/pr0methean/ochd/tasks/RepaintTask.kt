package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.DEFAULT_SNAPSHOT_PARAMS
import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.Retryer
import io.github.pr0methean.ochd.packedimage.ImagePacker
import javafx.scene.CacheHint
import javafx.scene.effect.Blend
import javafx.scene.effect.BlendMode
import javafx.scene.effect.ColorInput
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.paint.Paint
import kotlinx.coroutines.CoroutineScope
import org.apache.logging.log4j.util.StringBuilderFormattable
import org.apache.logging.log4j.util.Unbox
import java.lang.StringBuilder

data class RepaintTask(
    val paint: Paint?,
    val base: TextureTask,
    private val size: Int,
    val alpha: Double = 1.0,
    override val packer: ImagePacker,
    override val scope: CoroutineScope,
    override val stats: ImageProcessingStats,
    override val retryer: Retryer,
) : AbstractTextureTask(packer, scope, stats, retryer), StringBuilderFormattable {
    override suspend fun computeImage(): Image {
        val view = ImageView(base.getImage().unpacked())
        if (paint != null) {
            val colorLayer = ColorInput(0.0, 0.0, size.toDouble(), size.toDouble(), paint)
            val blend = Blend()
            blend.mode = BlendMode.SRC_ATOP
            blend.topInput = colorLayer
            blend.bottomInput = null
            view.effect = blend
        }
        view.opacity = alpha
        view.cacheHint = CacheHint.QUALITY
        view.isSmooth = true
        return doJfx {view.snapshot(DEFAULT_SNAPSHOT_PARAMS, null)}
    }

    override fun formatTo(buffer: StringBuilder) {
        buffer.append("RepaintTask(").append(base).append(',').append(paint).append(',')
            .append(Unbox.box(alpha)).append(")")
    }
}