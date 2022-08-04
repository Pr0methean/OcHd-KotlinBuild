package io.github.pr0methean.ochd.tasks.consumable

import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.tasks.consumable.caching.SoftTaskCache
import io.github.pr0methean.ochd.tasks.consumable.caching.TaskCache
import javafx.scene.image.Image

abstract class AbstractConsumableImageTask(override val name: String, cache: TaskCache<Image>,
                                           open val stats: ImageProcessingStats)
    : SimpleConsumableTask<Image>(name, cache), ConsumableImageTask {
    override val unpacked = this
    override suspend fun mergeWithDuplicate(other: ConsumableTask<Image>): ConsumableImageTask {
        return super.mergeWithDuplicate(other) as ConsumableImageTask
    }

    override val asPng by lazy { PngCompressionTask(this, SoftTaskCache(), stats) }
}