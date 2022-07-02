package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.LayerList
import javafx.geometry.Insets
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.image.WritableImage
import javafx.scene.layout.Background
import javafx.scene.layout.BackgroundFill
import javafx.scene.layout.CornerRadii
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import kotlinx.coroutines.CoroutineScope

data class ImageStackingTask(private val layers: LayerList,
                             private val size: Int,
                             private val scope: CoroutineScope): TextureTask(scope) {
    override suspend fun computeBitmap(): Image {
        val stackPane = StackPane()
        if (Color.TRANSPARENT != layers.background) {
            stackPane.background = Background(BackgroundFill(layers.background, CornerRadii.EMPTY, Insets.EMPTY))
        }
        stackPane.children.addAll(layers
            .map { layerTask ->
                ImageView(layerTask.getBitmap())
                .also { it.isSmooth = true } })
        val output = WritableImage(size, size)
        stackPane.snapshot(null, output)
        return output
    }
}