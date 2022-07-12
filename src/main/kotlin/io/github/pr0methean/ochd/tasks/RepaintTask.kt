package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.DEFAULT_SNAPSHOT_PARAMS
import io.github.pr0methean.ochd.ImageProcessingContext
import javafx.scene.CacheHint
import javafx.scene.effect.Blend
import javafx.scene.effect.BlendMode
import javafx.scene.effect.ColorInput
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.paint.Paint
import kotlinx.coroutines.runBlocking

data class RepaintTask(
    val paint: Paint?, val base: TextureTask, private val size: Int, val alpha: Double = 1.0,
    override val ctx: ImageProcessingContext
) : TextureTask(ctx) {
    override suspend fun computeImage(): Image {
        val view = ImageView(base.getImage().unpacked())
        isAllocated = true
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

    override fun willExpandHeap(): Boolean {
        return super.willExpandHeap() || base.willExpandHeap() ||
                (base.isComplete() && !runBlocking {base.getImage()}.isAlreadyUnpacked())
    }
}