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
    private val scope: CoroutineScope
)
    : TextureTask(scope) {

    override suspend fun computeBitmap(): Image {
        val colorLayer = ColorInput()
        colorLayer.paint = paint
        val blend = Blend()
        blend.mode = BlendMode.SRC_ATOP
        blend.opacity = alpha
        blend.topInput = colorLayer
        val view = ImageView(base.getBitmap())
        view.isSmooth = true
        view.effect = blend
        val output = WritableImage(size, size)
        view.snapshot(null, output)
        return output
    }
}