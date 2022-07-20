package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.Retryer
import io.github.pr0methean.ochd.packedimage.PackedImage
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.util.StringBuilderFormattable
import java.io.File

private val logger = LogManager.getLogger("OutputTask")
class OutputTask(val producer: Deferred<PackedImage>,
                 val name: String,
                 private val file: File,
                 val stats: ImageProcessingStats,
                 val retryer: Retryer,
): StringBuilderFormattable {

    suspend fun run() {
        stats.onTaskLaunched("OutputTask", "OutputTask for $name")
        val image: PackedImage
        try {
            image = producer.await()
        } catch (e: NotImplementedError) {
            logger.warn("Skipping $name because it's not implemented yet")
            return
        }
        withContext(Dispatchers.IO) {
            retryer.retrying(name) { image.writePng(file) }
        }
        stats.onTaskCompleted("OutputTask", "OutputTask for $name")
    }

    override fun toString(): String = name
    override fun formatTo(buffer: StringBuilder) {
        buffer.append(name)
    }
}