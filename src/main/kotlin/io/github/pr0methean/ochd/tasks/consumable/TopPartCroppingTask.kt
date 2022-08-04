package io.github.pr0methean.ochd.tasks.consumable

import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.tasks.consumable.caching.SoftTaskCache
import io.github.pr0methean.ochd.tasks.consumable.caching.TaskCache
import javafx.scene.image.Image
import javafx.scene.image.WritableImage

const val TOP_PORTION = 11.0/32
class TopPartCroppingTask(override val base: Task<Image>, override val name: String,
                          override val cache: TaskCache<Image>,
                          val stats: ImageProcessingStats): SlowTransformingTask<Image, Image>("Top part of $base", base, cache, { image ->
    val pixelReader = image.pixelReader
    WritableImage(pixelReader, image.width.toInt(), (image.height * TOP_PORTION).toInt())
}), ImageTask {
    override val unpacked: Task<Image> = this
    override val asPng: Task<ByteArray> = PngCompressionTask(this, SoftTaskCache(), stats)

    override fun equals(other: Any?): Boolean {
        return (other === this) || (other is TopPartCroppingTask && other.base == base)
    }

    override fun hashCode(): Int {
        return base.hashCode() - 13
    }

    override suspend fun mergeWithDuplicate(other: Task<Image>): ImageTask {
        return super.mergeWithDuplicate(other) as ImageTask
    }
}