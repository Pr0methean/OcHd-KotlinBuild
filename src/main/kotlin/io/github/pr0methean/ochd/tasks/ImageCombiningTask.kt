package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.ImageProcessingContext
import io.github.pr0methean.ochd.LayerList
import javafx.scene.image.Image

abstract class ImageCombiningTask(
    private val layers: LayerList, open val size: Int,
    ctx: ImageProcessingContext
) : JfxTextureTask<List<Image>>(ctx) {
    override suspend fun computeInput(): List<Image> {
        val output = ArrayList<Image>(layers.size)
        layers.forEach {output.add(it.getBitmap())}
        return output
    }
}