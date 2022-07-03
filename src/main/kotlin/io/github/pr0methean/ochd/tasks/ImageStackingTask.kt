package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.LayerList
import javafx.scene.SnapshotParameters
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.image.WritableImage
import javafx.scene.layout.Background
import javafx.scene.layout.BackgroundFill
import javafx.scene.layout.StackPane
import kotlinx.coroutines.CoroutineScope

data class ImageStackingTask(
    val layers: LayerList,
    val size: Int,
    override val scope: CoroutineScope): ImageCombiningTask(layers, size, scope) {

    override fun doBlockingJfx(input: List<Image>): Image {
        val stackPane = StackPane()
        stackPane.background = Background(BackgroundFill(layers.background, null, null))
        stackPane.children.addAll(input.map {ImageView(it).also {
            it.isSmooth = true
        }})
        val params = SnapshotParameters()
        params.fill = layers.background
        val output = WritableImage(size, size)
        stackPane.snapshot(params, output)
        return output
    }

}