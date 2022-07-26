package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.packedimage.ImagePacker
import io.github.pr0methean.ochd.packedimage.PackedImage
import javafx.scene.image.Image
import kotlinx.coroutines.CoroutineScope

abstract class UnpackingTextureTask(
    open val packer: ImagePacker, scope: CoroutineScope, override val stats: ImageProcessingStats
) : AbstractTextureTask(scope, stats) {

    override suspend fun createImage(): PackedImage = packer.packImage(computeImage(), null, name)

    abstract suspend fun computeImage(): Image

}