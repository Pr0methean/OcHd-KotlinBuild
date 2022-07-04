package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.DEFAULT_SNAPSHOT_PARAMS
import io.github.pr0methean.ochd.ImageProcessingContext
import javafx.scene.CacheHint
import javafx.scene.effect.Blend
import javafx.scene.effect.BlendMode
import javafx.scene.effect.ColorInput
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.image.WritableImage
import javafx.scene.paint.Paint

data class RepaintTask(
    private val paint: Paint?, private val base: TextureTask, private val size: Int, val alpha: Double = 1.0,
    val ctx: ImageProcessingContext
)
    : JfxTextureTask<Image>(ctx) {

    override suspend fun computeInput(): Image = base.getImage()

    override fun doBlockingJfx(input: Image): Image {
        val view = ImageView(input)
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
        val output = WritableImage(size, size)
        retryOnOomBlocking { view.snapshot(DEFAULT_SNAPSHOT_PARAMS, output) }
        return output
    }
}