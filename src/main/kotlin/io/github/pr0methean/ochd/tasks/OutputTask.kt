package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.Retryer
import io.github.pr0methean.ochd.packedimage.ImageNode
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.logging.log4j.LogManager
import java.io.File

private val logger = LogManager.getLogger("OutputTask")
class OutputTask(private val producer: TextureTask,
                 override val name: String,
                 private val file: File,
                 val stats: ImageProcessingStats,
                 val retryer: Retryer,
): Task {
    @Volatile private var started: Boolean = false
    @Volatile private var completed: Boolean = false

    override suspend fun run() {
        invoke()
    }

    private suspend fun invoke() {
        do {
            stats.onTaskLaunched("OutputTask", name)
            val image: ImageNode
            started = true
            try {
                image = retryer.retrying(producer.toString()) { producer.getImage() }
            } catch (e: NotImplementedError) {
                logger.warn("Skipping $name because it's not implemented yet")
                completed = true
                return
            }
            withContext(Dispatchers.IO.plus(CoroutineName(name))) {
                retryer.retrying(name) { image.writePng(file) }
            }
            if (!file.exists()) {
                logger.error("OutputTask $name appeared to succeed, but $file still doesn't exist")
            }
        } while (!file.exists())
        completed = true
        stats.onTaskCompleted("OutputTask", name)
    }

    override fun isComplete(): Boolean = completed

    override fun isStarted(): Boolean = started

    override fun dependencies(): Collection<Task> = listOf(producer)

    override fun toString(): String = "Output of $name"
    override fun formatTo(buffer: StringBuilder) {
        buffer.append("Output of ").append(name)
    }
}