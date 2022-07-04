package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.ImageProcessingContext
import io.github.pr0methean.ochd.LayerList
import javafx.scene.image.Image
import kotlinx.coroutines.CoroutineScope

abstract class ImageCombiningTask(
    private val layers: LayerList, open val size: Int,
    scope: CoroutineScope,
    ctx: ImageProcessingContext
) : JfxTextureTask<List<Image>>(scope, ctx) {
    override suspend fun computeInput(): List<Image> {
        val output = ArrayList<Image>(layers.size)
        layers.forEach {output.add(it.getBitmap())}
        return output
    }
}