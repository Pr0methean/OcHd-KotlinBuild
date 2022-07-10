package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.ImageProcessingContext
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import java.util.*

abstract class OutputTask(open val name: String, override val ctx: ImageProcessingContext): RetryableTask<Unit>(ctx) {
    // Lazy init is needed to work around an NPE bug
    val file by lazy {ctx.outTextureRoot.resolve(name.lowercase(Locale.ENGLISH) + ".png")}
    override fun createCoroutineAsync() = ctx.scope.async(start = CoroutineStart.LAZY) {
        ctx.taskLaunches.add(this::class.simpleName ?: "[unnamed OutputTask subclass]")
        println("Starting output task for $name")
        withContext(Dispatchers.IO) {invoke()}
        println("Finished output task for $name")
    }
    protected abstract suspend fun invoke()
}