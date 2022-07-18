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
class OutputTask(producer: TextureTask,
                      val name: String,
                      val file: File,
                      val semaphore: Semaphore?,
                      val stats: ImageProcessingStats,
                      val retryer: Retryer,
): StringBuilderFormattable {
    @Volatile
    var producer: TextureTask? = producer

    suspend fun invoke() {
        stats.onTaskLaunched(this@OutputTask)
        val image: PackedImage
        try {
            image = retryer.retrying(name) {producer!!.getImage()}
        } catch (e: NotImplementedError) {
            logger.warn("Skipping $name because it's not implemented yet")
            return
        }
        producer = null
        image.writePng(file)
        stats.onTaskCompleted(this@OutputTask)
    }
    suspend fun run() {
        withContext(Dispatchers.IO) {
            file.parentFile.mkdirs()
            if (semaphore != null && !producer!!.isStarted()) {
                semaphore.withPermit {
                    invoke()
                }
            } else {
                invoke()
            }
        }
    }

    override fun toString(): String = "Output of $name"
    override fun formatTo(buffer: StringBuilder) {
        buffer.append("Output of ").append(name)
    }
}