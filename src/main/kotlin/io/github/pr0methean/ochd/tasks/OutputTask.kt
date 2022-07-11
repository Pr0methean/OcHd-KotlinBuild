package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.ImageProcessingContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

abstract class OutputTask(open val name: String, open val ctx: ImageProcessingContext) {

    // Lazy init is needed to work around an NPE bug
    val file by lazy {ctx.outTextureRoot.resolve(name.lowercase(Locale.ENGLISH) + ".png")}
    protected abstract suspend fun invoke()
    suspend fun run() = withContext(Dispatchers.IO) {
        ctx.onTaskLaunched(this@OutputTask)
        ctx.retrying {invoke()}
        ctx.onTaskCompleted(this@OutputTask)
    }
}