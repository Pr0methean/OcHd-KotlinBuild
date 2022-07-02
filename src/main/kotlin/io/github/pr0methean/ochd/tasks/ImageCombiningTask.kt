package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.LayerList
import javafx.scene.image.Image
import kotlinx.coroutines.CoroutineScope

abstract class ImageCombiningTask(
    private val layers: LayerList, private val size: Int,
    scope: CoroutineScope
) : JfxTextureTask<List<Image>>(scope) {
    override suspend fun computeInput(): List<Image> {
        val output = ArrayList<Image>(layers.size)
        layers.forEach {output.add(it.getBitmap())}
        return output
    }
}