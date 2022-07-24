package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.Retryer
import io.github.pr0methean.ochd.packedimage.ImageNode
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.util.StringBuilderFormattable
import java.io.File

private val logger = LogManager.getLogger("OutputTask")
class OutputTask(val producer: TextureTask,
                 val name: String,
                 private val file: File,
                 private val semaphore: Semaphore?,
                 val stats: ImageProcessingStats,
                 val retryer: Retryer,
): StringBuilderFormattable {

    suspend fun run() {
        if (semaphore == null || producer.isStarted()) {
            invoke()
        } else {
            semaphore.withPermit{invoke()}
        }
    }

    private suspend fun invoke() {
        do {
            stats.onTaskLaunched("OutputTask", name)
            val image: ImageNode
            try {
                image = retryer.retrying(producer.toString()) { producer.getImage() }
            } catch (e: NotImplementedError) {
                logger.warn("Skipping $name because it's not implemented yet")
                return
            }
            withContext(Dispatchers.IO.plus(CoroutineName(name))) {
                retryer.retrying(name) { image.writePng(file) }
            }
            if (!file.exists()) {
                logger.error("OutputTask $name appeared to succeed, but $file still doesn't exist")
            }
        } while (!file.exists())
        stats.onTaskCompleted("OutputTask", name)
    }

    override fun toString(): String = "Output of $name"
    override fun formatTo(buffer: StringBuilder) {
        buffer.append("Output of ").append(name)
    }
}