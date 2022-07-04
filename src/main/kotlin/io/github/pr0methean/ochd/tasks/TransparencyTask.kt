package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.DEFAULT_SNAPSHOT_PARAMS
import io.github.pr0methean.ochd.ImageProcessingContext
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.image.WritableImage

data class TransparencyTask(
    val base: JfxTextureTask<*>,
    val size: Int,
    val alpha: Double,
    val ctx: ImageProcessingContext
)
        : JfxTextureTask<Image>(ctx) {
    override suspend fun computeInput(): Image = base.getBitmap()

    override fun doBlockingJfx(input: Image): Image {
        val view = ImageView(input)
        view.opacity = alpha
        val output = WritableImage(size, size)
        retryOnOomBlocking { view.snapshot(DEFAULT_SNAPSHOT_PARAMS, output) }
        return output
    }

}
