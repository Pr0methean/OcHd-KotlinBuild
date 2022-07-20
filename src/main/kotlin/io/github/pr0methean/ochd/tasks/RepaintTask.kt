package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.DEFAULT_SNAPSHOT_PARAMS
import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.Retryer
import io.github.pr0methean.ochd.packedimage.ImagePacker
import io.github.pr0methean.ochd.packedimage.PackedImage
import javafx.scene.CacheHint
import javafx.scene.effect.Blend
import javafx.scene.effect.BlendMode
import javafx.scene.effect.ColorInput
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.paint.Paint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred

data class RepaintTask(
    val paint: Paint?,
    val base: Deferred<PackedImage>,
    private val size: Int,
    val alpha: Double = 1.0,
    override val packer: ImagePacker,
    override val scope: CoroutineScope,
    override val stats: ImageProcessingStats,
    override val retryer: Retryer,
) : AbstractTextureTask(packer, scope, stats, retryer) {
    override suspend fun computeImage(): Image {
        val view = ImageView(base.await().unpacked())
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
        return doJfx(name, retryer) {view.snapshot(DEFAULT_SNAPSHOT_PARAMS, null)}
    }

    override fun formatTo(buffer: StringBuilder) {
        buffer.append(base)
        if (paint != null) {
            buffer.append(" painted ").append(paint)
        }
        if (alpha != 1.0) {
            buffer.append(" with alpha ").append(alpha)
        }
    }
}