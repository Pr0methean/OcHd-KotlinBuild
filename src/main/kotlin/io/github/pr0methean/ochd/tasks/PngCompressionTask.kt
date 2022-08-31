package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.tasks.caching.TaskCache
import javafx.embed.swing.SwingFXUtils
import javafx.scene.image.Image
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

class PngCompressionTask(
    override val base: AbstractTask<Image>, override val cache: TaskCache<ByteArray>, val stats: ImageProcessingStats
): TransformingTask<Image, ByteArray>("PNG compression of $base", base = base, cache = cache, transform = { image ->
    ByteArrayOutputStream().use {
        stats.onTaskLaunched("PngCompressionTask", base.name)
        @Suppress("BlockingMethodInNonBlockingContext")

        ImageIO.write(SwingFXUtils.fromFXImage(image, null), "PNG", it)
        val packed = it.toByteArray()
        stats.onTaskCompleted("PngCompressionTask", base.name)
        packed
    }
}) {
    override fun equals(other: Any?): Boolean {
        return (this === other) || (other is PngCompressionTask && other.base == base)
    }

    override fun hashCode(): Int {
        return base.hashCode() + 17
    }
}