package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.Retryer
import io.github.pr0methean.ochd.packedimage.PackedImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.util.StringBuilderFormattable
import java.io.File
import java.lang.StringBuilder

private val logger = LogManager.getLogger("OutputTask")
class OutputTask(val producer: TextureTask,
                 val name: String,
                 private val file: File,
                 private val semaphore: Semaphore?,
                 val stats: ImageProcessingStats,
                 val retryer: Retryer,
): StringBuilderFormattable {

    suspend fun invoke() {
        stats.onTaskLaunched(this@OutputTask)
        val image: PackedImage
        try {
            image = retryer.retrying(producer.toString()) { producer.getImage()}
        } catch (e: NotImplementedError) {
            logger.warn("Skipping $name because it's not implemented yet")
            return
        }
        retryer.retrying(name) {image.writePng(file)}
        stats.onTaskCompleted(this@OutputTask)
    }
    suspend fun run(onCompletion: (OutputTask) -> Unit) {
        withContext(Dispatchers.IO) {
            file.parentFile.mkdirs()
            if (semaphore != null && !producer.isStarted()) {
                semaphore.withPermit {
                    invoke()
                }
            } else {
                invoke()
            }
        }
        onCompletion(this)
    }

    override fun toString(): String = "Output of $name"
    override fun formatTo(buffer: StringBuilder) {
        buffer.append("Output of ").append(name)
    }
}