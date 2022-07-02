package io.github.pr0methean.ochd.tasks

import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.image.WritableImage
import kotlinx.coroutines.CoroutineScope

data class TransparencyTask(val base: io.github.pr0methean.ochd.tasks.JfxTextureTask<*>, val size: Int, val alpha: Double, override val scope: CoroutineScope)
        : JfxTextureTask<Image>(scope) {
    override suspend fun computeInput(): Image = base.getBitmap()

    override fun doBlockingJfx(input: Image): Image {
        val view = ImageView()
        view.opacity = alpha
        val output = WritableImage(size, size)
        view.snapshot(null, output)
        return output
    }

}
