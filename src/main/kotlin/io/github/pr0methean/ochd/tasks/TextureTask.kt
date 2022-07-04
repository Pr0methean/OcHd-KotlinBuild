package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.ImageProcessingContext
import javafx.scene.image.Image
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async

abstract class TextureTask(ctx: ImageProcessingContext) {
    private val coroutine = ctx.scope.async(start = CoroutineStart.LAZY) {
        println("Starting task ${this@TextureTask}")
        ctx.taskLaunches.add(this@TextureTask::class.simpleName)
        val bitmap = computeBitmap()
        println("Finished task ${this@TextureTask}")
        return@async ctx.packImage(bitmap)
    }

    abstract suspend fun computeBitmap(): Image

    suspend fun getBitmap(): Image = coroutine.await()()
}