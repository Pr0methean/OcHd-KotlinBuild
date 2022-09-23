package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.tasks.caching.TaskCache
import io.github.pr0methean.ochd.tasks.caching.noopTaskCache
import javafx.scene.image.Image
import java.util.*

abstract class AbstractImageTask(override val name: String, cache: TaskCache<Image>,
                                 open val stats: ImageProcessingStats)
    : SimpleTask<Image>(name, cache), ImageTask {
    override suspend fun mergeWithDuplicate(other: Task<Image>): ImageTask {
        if (other is AbstractImageTask) {
            opaqueRepaints.addAll(other.opaqueRepaints)
        }
        return super.mergeWithDuplicate(other) as ImageTask
    }

    private val opaqueRepaints = Collections.newSetFromMap(WeakHashMap<ImageTask,Boolean>())

    override fun opaqueRepaints(): Iterable<ImageTask> = opaqueRepaints

    override fun addOpaqueRepaint(repaint: ImageTask) {
        opaqueRepaints.add(repaint)
    }

    override val asPng: PngCompressionTask by lazy { PngCompressionTask(this, noopTaskCache(), stats) }
}