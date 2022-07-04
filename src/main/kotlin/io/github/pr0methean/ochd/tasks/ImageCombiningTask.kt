package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.ImageProcessingContext
import io.github.pr0methean.ochd.LayerList
import io.github.pr0methean.ochd.packedimage.PackedImage

abstract class ImageCombiningTask(
    private val layerList: LayerList, open val size: Int,
    ctx: ImageProcessingContext
) : JfxTextureTask<List<PackedImage>>(ctx) {
    override suspend fun computeInput(): List<PackedImage> {
        val output = ArrayList<PackedImage>(layerList.layers.size)
        layerList.layers.forEach {output.add(it.getPackedImage())}
        return output
    }
}