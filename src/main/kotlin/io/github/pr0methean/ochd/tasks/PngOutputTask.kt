package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.tasks.caching.noopDeferredTaskCache
import javafx.embed.swing.SwingFXUtils
import javafx.scene.image.Image
import java.io.File
import java.nio.file.Files
import javax.imageio.ImageIO
import kotlin.coroutines.CoroutineContext

/**
 * Task that saves an image to one or more PNG files.
 */
@Suppress("BlockingMethodInNonBlockingContext")
class PngOutputTask(
    name: String,
    base: Task<Image>,
    private val files: List<File>,
    ctx: CoroutineContext,
    private val stats: ImageProcessingStats,
): TransformingTask<Image, Unit>("Output $name", base, noopDeferredTaskCache(), ctx) {
    override suspend fun transform(input: Image) {
        stats.onTaskLaunched("PngOutputTask", name)
        val firstFile = files[0]
        firstFile.parentFile?.mkdirs()
        val firstFilePath = firstFile.absoluteFile.toPath()
        ImageIO.write(SwingFXUtils.fromFXImage(input, null), "PNG", firstFile)
        if (files.size > 1) {
            for (file in files.subList(1, files.size)) {
                file.parentFile?.mkdirs()
                Files.copy(firstFilePath, file.absoluteFile.toPath())
            }
        }
        stats.onTaskCompleted("PngOutputTask", name)
    }

    val isCommandBlock: Boolean = name.contains("command_block")

    init {
        check(files.isNotEmpty()) { "PngOutputTask $name has no destination files" }
    }
}
