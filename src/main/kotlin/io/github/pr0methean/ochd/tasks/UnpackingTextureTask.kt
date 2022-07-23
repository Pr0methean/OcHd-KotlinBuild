package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.Retryer
import io.github.pr0methean.ochd.packedimage.ImageNode
import io.github.pr0methean.ochd.packedimage.ImagePacker
import javafx.scene.image.Image
import kotlinx.coroutines.CoroutineScope

abstract class UnpackingTextureTask(
    open val packer: ImagePacker, scope: CoroutineScope, override val stats: ImageProcessingStats,
    open val retryer: Retryer) : AbstractTextureTask(scope, stats) {

    override suspend fun createImage(): ImageNode = packer.packImage(computeImage(), null, name)

    protected suspend fun <T> doJfx(jfxCode: () -> T): T
            = doJfx(name, retryer, jfxCode)

    abstract suspend fun computeImage(): Image

}