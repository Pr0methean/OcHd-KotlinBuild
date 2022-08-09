package io.github.pr0methean.ochd.tasks.consumable

import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.tasks.consumable.caching.TaskCache
import io.github.pr0methean.ochd.tasks.consumable.caching.noopTaskCache
import javafx.scene.image.Image

abstract class AbstractImageTask(override val name: String, cache: TaskCache<Image>,
                                 open val stats: ImageProcessingStats)
    : SimpleTask<Image>(name, cache), ImageTask {
    @Suppress("LeakingThis")
    override val unpacked: AbstractImageTask = this
    override suspend fun mergeWithDuplicate(other: Task<Image>): ImageTask {
        return super.mergeWithDuplicate(other) as ImageTask
    }

    override val asPng: PngCompressionTask by lazy { PngCompressionTask(this, noopTaskCache(), stats) }
}