package io.github.pr0methean.ochd.tasks.consumable

import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.tasks.consumable.caching.StrongTaskCache
import io.github.pr0methean.ochd.tasks.consumable.caching.TaskCache
import javafx.scene.image.Image

abstract class AbstractConsumableImageTask(override val name: String, cache: TaskCache<Image>,
                                           open val stats: ImageProcessingStats)
    : SimpleConsumableTask<Image>(name, cache), ConsumableImageTask {
    override val unpacked = this
    override val asPng by lazy {PngCompressionTask(this, StrongTaskCache(), stats)}
}