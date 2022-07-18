package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.ImageProcessingContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.util.StringBuilderFormattable
import java.lang.StringBuilder

private val logger = LogManager.getLogger("OutputTask")
data class OutputTask(private val producer: TextureTask,
                      val name: String,
                      val ctx: ImageProcessingContext
): StringBuilderFormattable {

    // Lazy init is needed to work around an NPE bug
    private val file by lazy {ctx.outTextureRoot.resolve("$name.png")}
    suspend fun invoke() {
        try {
            val image = producer.getImage()
            image.writePng(file)
        } catch (e: NotImplementedError) {
            logger.warn("Skipping $name because it's not implemented yet")
        }
    }
    suspend fun run() {
        withContext(Dispatchers.IO) {
            file.parentFile.mkdirs()
            if (ctx.needSemaphore && producer.willExpandHeap()) {
                ctx.newTasksSemaphore.withPermit {
                    ctx.onTaskLaunched(this@OutputTask)
                    ctx.retrying(name) {invoke()}
                }
            } else {
                ctx.onTaskLaunched(this@OutputTask)
                ctx.retrying(name) {invoke()}
            }
            ctx.onTaskCompleted(this@OutputTask)
        }
    }

    override fun toString(): String = "Output of $name"
    override fun formatTo(buffer: StringBuilder) {
        buffer.append("Output of ").append(name)
    }
}