package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.LayerList
import io.github.pr0methean.ochd.Retryer
import io.github.pr0methean.ochd.packedimage.ImagePacker
import io.github.pr0methean.ochd.packedimage.superimpose
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList

data class ImageStackingTask(
    val layers: LayerList,
    val size: Int,
    val packer: ImagePacker,
    override val scope: CoroutineScope,
    override val stats: ImageProcessingStats,
    val retryer: Retryer
): AbstractTextureTask(scope, stats) {

    override suspend fun createImage() = superimpose(
        layers.background,
        layers.layers.asFlow().map {it.getImage()}.toList(),
        size.toDouble(), size.toDouble(), layers.toString(), retryer, packer
    )

    override fun formatTo(buffer: StringBuilder) {
        layers.formatTo(buffer)
    }
}