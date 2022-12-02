package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.tasks.caching.TaskCache
import javafx.embed.swing.SwingFXUtils
import javafx.scene.image.Image
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asContextElement
import kotlinx.coroutines.plus
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

const val PNG_PRESIZE: Int = 512*1024
private val THREAD_LOCAL_BAOS = ThreadLocal.withInitial {ByteArrayOutputStream(PNG_PRESIZE)}
@Suppress("FunctionName")
class PngCompressionTask(
    base: AbstractTask<Image>, cache: TaskCache<ByteArray>, stats: ImageProcessingStats
): TransformingTask<Image, ByteArray>(
    "PNG compression of $base", base = base, cache = cache, transform = { image ->
    THREAD_LOCAL_BAOS.get().run {
        try {
            stats.onTaskLaunched("PngCompressionTask", base.name)
            @Suppress("BlockingMethodInNonBlockingContext")

            ImageIO.write(SwingFXUtils.fromFXImage(image, null), "PNG", this)
            val packed = toByteArray()
            stats.onTaskCompleted("PngCompressionTask", base.name)
            packed
        } finally {
            reset()
        }
    }
}) {
    override suspend fun createCoroutineScope(): CoroutineScope {
        return super.createCoroutineScope().plus(THREAD_LOCAL_BAOS.asContextElement())
    }
}