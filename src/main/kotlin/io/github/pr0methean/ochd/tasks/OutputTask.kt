package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.ImageProcessingContext
import java.util.*

abstract class OutputTask(open val name: String, open val ctx: ImageProcessingContext) {
    val file = ctx.outTextureRoot.resolve(name.lowercase(Locale.ENGLISH) + ".png")
    protected abstract suspend fun invoke()
    suspend fun run() {
        ctx.taskLaunches.add(this::class.simpleName)
        println("Starting output task for $name")
        invoke()
        println("Finished output task for $name")
    }
}