package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.ImageProcessingContext
import io.github.pr0methean.ochd.LayerList
import io.github.pr0methean.ochd.packedimage.PackedImage
import javafx.scene.SnapshotParameters
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.image.WritableImage
import javafx.scene.layout.Background
import javafx.scene.layout.BackgroundFill
import javafx.scene.layout.StackPane

data class ImageStackingTask(
    val layers: LayerList,
    override val size: Int,
    override val ctx: ImageProcessingContext
): ImageCombiningTask(layers, size, ctx) {

    override fun doBlockingJfx(input: List<PackedImage>): Image {
        val stackPane = StackPane()
        stackPane.background = Background(BackgroundFill(layers.background, null, null))
        stackPane.children.addAll(input.map {ImageView(it.unpack()).also {
            it.isSmooth = true
        }})
        val params = SnapshotParameters()
        params.fill = layers.background
        val output = WritableImage(size, size)
        retryOnOomBlocking { stackPane.snapshot(params, output) }
        return output
    }

}