package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.ImageProcessingContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.util.logging.Logger

private val logger = Logger.getLogger("OutputTask")
data class OutputTask(private val producer: TextureTask,
                      val name: String,
                      val ctx: ImageProcessingContext
) {

    // Lazy init is needed to work around an NPE bug
    private val file by lazy {ctx.outTextureRoot.resolve("$name.png")}
    suspend fun invoke() {
        try {
            val image = producer.getImage()
            image.writePng(file)
        } catch (e: NotImplementedError) {
            logger.warning("Skipping $name because it's not implemented yet")
        }
    }
    suspend fun run() {
        withContext(Dispatchers.IO) {
            file.parentFile.mkdirs()
            if (ctx.needSemaphore && (producer.willExpandHeap() || producer.getImageNow()?.isAlreadyPacked() != true)) {
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
}