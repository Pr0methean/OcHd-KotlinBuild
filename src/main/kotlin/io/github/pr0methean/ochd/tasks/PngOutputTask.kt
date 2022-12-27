package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.tasks.caching.noopDeferredTaskCache
import javafx.embed.swing.SwingFXUtils
import javafx.scene.image.Image
import kotlinx.coroutines.asContextElement
import kotlinx.coroutines.withContext
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Files
import java.util.concurrent.ConcurrentHashMap
import javax.imageio.ImageIO
import kotlin.coroutines.CoroutineContext

private val mkdirsedPaths = ConcurrentHashMap.newKeySet<File>()
private val threadLocalBimg: ThreadLocal<BufferedImage?> = ThreadLocal.withInitial { null }

/**
 * Task that saves an image to one or more PNG files.
 */
@Suppress("BlockingMethodInNonBlockingContext")
class PngOutputTask(
    name: String,
    val base: AbstractTask<Image>,
    private val files: List<File>,
    ctx: CoroutineContext,
    private val stats: ImageProcessingStats,
): AbstractTask<Unit>("Output $name", noopDeferredTaskCache(), ctx) {
    override val directDependencies: Iterable<AbstractTask<*>> = listOf(base)

    override suspend fun perform() {
        withContext(threadLocalBimg.asContextElement()) {
            val oldImage = threadLocalBimg.get()
            val baseTask = base.start()
            stats.onTaskLaunched("PngOutputTask", name)
            files.mapNotNull(File::getParentFile).distinct().forEach { parent ->
                if (mkdirsedPaths.add(parent)) {
                    parent.mkdirs()
                }
            }
            val firstFile = files[0]
            val firstFilePath = firstFile.absoluteFile.toPath()
            val fxImage = baseTask.await()
            val bImg: BufferedImage
            if (oldImage == null) {
                bImg = SwingFXUtils.fromFXImage(fxImage, null)
                if (!isCommandBlock && bImg.width == bImg.height) {
                    threadLocalBimg.set(bImg)
                }
            } else {
                bImg = SwingFXUtils.fromFXImage(
                    fxImage,
                    if (fxImage.height.toInt() == oldImage.height && fxImage.width.toInt() == oldImage.width) {
                        oldImage
                    } else null
                )
            }
            ImageIO.write(bImg, "PNG", firstFile)
            base.removeDirectDependentTask(this@PngOutputTask)
            if (files.size > 1) {
                for (file in files.subList(1, files.size)) {
                    Files.createLink(file.absoluteFile.toPath(), firstFilePath)
                }
            }
        }
        stats.onTaskCompleted("PngOutputTask", name)
    }

    val isCommandBlock: Boolean = name.contains("command_block")

    init {
        check(files.isNotEmpty()) { "PngOutputTask $name has no destination files" }
    }
}
