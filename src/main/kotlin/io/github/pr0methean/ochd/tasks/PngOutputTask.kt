package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.tasks.caching.noopDeferredTaskCache
import javafx.embed.swing.SwingFXUtils
import javafx.scene.image.Image
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import org.apache.logging.log4j.LogManager
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Files
import java.util.concurrent.ConcurrentHashMap
import javax.imageio.ImageIO
import kotlin.coroutines.CoroutineContext

private val logger = LogManager.getLogger("PngOutputTask")
val mkdirsedPaths = ConcurrentHashMap.newKeySet<File>()
private val threadLocalBimg: ThreadLocal<BufferedImage?> = ThreadLocal.withInitial { null }

/**
 * Task that saves an image to one or more PNG files.
 */
@Suppress("BlockingMethodInNonBlockingContext", "EqualsWithHashCodeExist", "EqualsOrHashCode")
class PngOutputTask(
    name: String,
    val base: AbstractTask<Image>,
    val files: List<File>,
    ctx: CoroutineContext,
): AbstractTask<Unit>("Output $name", noopDeferredTaskCache(), ctx) {
    override val directDependencies: Iterable<AbstractTask<*>> = listOf(base)
    private val ioScope = coroutineScope.plus(Dispatchers.IO)
    override fun mergeWithDuplicate(other: AbstractTask<*>): AbstractTask<Unit> {
        if (other is PngOutputTask && other !== this && other.base !== base) {
            logger.debug("Merging PngOutputTask {} with duplicate {}", name, other.name)
            val mergedBase = base.mergeWithDuplicate(other.base)
            if (mergedBase !== base || files.toSet() != other.files.toSet()) {
                return PngOutputTask(
                    name,
                    mergedBase,
                    files.union(other.files).toList(),
                    ctx
                )
            }
        }
        return super.mergeWithDuplicate(other)
    }

    @Suppress("MagicNumber")
    override fun computeHashCode(): Int = base.hashCode() - 127

    override fun equals(other: Any?): Boolean {
        return (this === other) || other is PngOutputTask && base == other.base
    }

    override suspend fun perform() {
        val baseTask = base.start()
        ImageProcessingStats.onTaskLaunched("PngOutputTask", name)
        val fxImage = baseTask.await()
        writeToFiles(fxImage)
        ImageProcessingStats.onTaskCompleted("PngOutputTask", name)
    }

    suspend fun writeToFiles(fxImage: Image): Job {
        val oldImage = threadLocalBimg.get()
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
        val firstFile = files[0]
        val firstFilePath = firstFile.absoluteFile.toPath()
        val writeFirstFile = ioScope.launch {
            ImageIO.write(bImg, "PNG", firstFile)
        }
        return if (files.size > 1) {
            ioScope.launch {
                val remainingFiles = files.subList(1, files.size)
                writeFirstFile.join()
                for (file in remainingFiles) {
                    Files.createLink(file.absoluteFile.toPath(), firstFilePath)
                }
            }
        } else {
            writeFirstFile
        }
    }

    val isCommandBlock: Boolean = name.contains("command_block")

    init {
        check(files.isNotEmpty()) { "PngOutputTask $name has no destination files" }
    }
}
