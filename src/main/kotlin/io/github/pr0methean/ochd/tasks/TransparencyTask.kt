package io.github.pr0methean.ochd.tasks

import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.image.WritableImage
import kotlinx.coroutines.CoroutineScope

data class TransparencyTask(val base: TextureTask, val size: Int, val alpha: Double, val scope: CoroutineScope) : TextureTask(scope) {
    override suspend fun computeBitmap(): Image {
        val view = ImageView(base.getBitmap())
        view.opacity = alpha
        val output = WritableImage(size, size)
        view.snapshot(null, output)
        return output
    }

}
