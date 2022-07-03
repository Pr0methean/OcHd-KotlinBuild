package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.DEFAULT_SNAPSHOT_PARAMS
import javafx.scene.CacheHint
import javafx.scene.effect.Blend
import javafx.scene.effect.BlendMode
import javafx.scene.effect.ColorInput
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.image.WritableImage
import javafx.scene.paint.Paint
import kotlinx.coroutines.CoroutineScope

data class RepaintTask(
    private val paint: Paint, private val base: TextureTask, private val size: Int, val alpha: Double = 1.0,
    override val scope: CoroutineScope
)
    : JfxTextureTask<Image>(scope) {

    override suspend fun computeInput(): Image = base.getBitmap()

    override fun doBlockingJfx(input: Image): Image {
        val colorLayer = ColorInput(0.0, 0.0, size.toDouble(), size.toDouble(), paint)
        val blend = Blend()
        blend.mode = BlendMode.SRC_ATOP
        blend.opacity = alpha
        blend.topInput = colorLayer
        blend.bottomInput = null
        val view = ImageView(input)
        view.effect = blend
        view.cacheProperty().set(true)
        view.cacheHint = CacheHint.QUALITY
        view.isSmooth = true
        val output = WritableImage(size, size)
        view.snapshot(DEFAULT_SNAPSHOT_PARAMS, output)
        return output
    }
}