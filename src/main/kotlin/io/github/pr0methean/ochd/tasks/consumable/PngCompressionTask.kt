package io.github.pr0methean.ochd.tasks.consumable

import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.tasks.consumable.caching.TaskCache
import javafx.embed.swing.SwingFXUtils
import javafx.scene.image.Image
import org.apache.logging.log4j.LogManager
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

private val logger = LogManager.getLogger("PngCompressionTask")
class PngCompressionTask(
    override val base: AbstractConsumableTask<Image>, override val cache: TaskCache<ByteArray>, val stats: ImageProcessingStats
): TransformingConsumableTask<Image, ByteArray>("PNG compression of $base", base = base, cache = cache, transform = { image ->
    ByteArrayOutputStream().use {
        stats.onCompressPngImage(base.name)
        @Suppress("BlockingMethodInNonBlockingContext")
        ImageIO.write(SwingFXUtils.fromFXImage(image, null), "PNG", it)
        val packed = it.toByteArray()
        logger.info("Done compressing {}", base.name)
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