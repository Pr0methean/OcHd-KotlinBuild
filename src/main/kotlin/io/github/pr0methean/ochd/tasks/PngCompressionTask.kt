package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.tasks.caching.TaskCache
import javafx.embed.swing.SwingFXUtils
import javafx.scene.image.Image
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

const val PNG_PRESIZE: Int = 4096
@Suppress("FunctionName")
fun PngCompressionTask(
    base: AbstractTask<Image>, cache: TaskCache<ByteArray>, stats: ImageProcessingStats
): TransformingTask<Image, ByteArray> = TransformingTask(
    "PNG compression of $base", base = base, cache = cache, transform = { image ->
    ByteArrayOutputStream(PNG_PRESIZE).use {
        stats.onTaskLaunched("PngCompressionTask", base.name)
        @Suppress("BlockingMethodInNonBlockingContext")

        ImageIO.write(SwingFXUtils.fromFXImage(image, null), "PNG", it)
        val packed = it.toByteArray()
        stats.onTaskCompleted("PngCompressionTask", base.name)
        packed
    }
})