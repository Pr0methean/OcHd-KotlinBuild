package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.ImageProcessingContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

data class OutputTask(private val producer: TextureTask,
                      val name: String,
                      val ctx: ImageProcessingContext
) {

    // Lazy init is needed to work around an NPE bug
    val file by lazy {ctx.outTextureRoot.resolve(name.lowercase(Locale.ENGLISH) + ".png")}
    suspend fun invoke() {
        try {
            producer.getImage().writePng(file)
        } catch (e: NotImplementedError) {
            println("Skipping $name because it's not implemented yet")
        }
    }
    suspend fun run() {
        withContext(Dispatchers.IO) {
            ctx.onTaskLaunched(this@OutputTask)
            file.parentFile.mkdirs()
        }
        ctx.retrying(name) {invoke()}
        ctx.onTaskCompleted(this@OutputTask)
    }

    override fun toString(): String = "OutputTask for $name"
}