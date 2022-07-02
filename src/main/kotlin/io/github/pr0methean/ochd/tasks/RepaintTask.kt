package io.github.pr0methean.ochd.tasks

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
        val colorLayer = ColorInput()
        colorLayer.paint = paint
        val blend = Blend()
        blend.mode = BlendMode.SRC_ATOP
        blend.opacity = alpha
        blend.topInput = colorLayer
        val view = ImageView(input)
        view.isSmooth = true
        view.effect = blend
        val output = WritableImage(size, size)
        view.snapshot(null, output)
        return output
    }
}