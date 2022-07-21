package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.Retryer
import io.github.pr0methean.ochd.packedimage.PackedImage
import javafx.application.Platform
import javafx.concurrent.Task
import kotlinx.coroutines.*

abstract class AbstractTextureTask(open val scope: CoroutineScope,
                                   open val stats: ImageProcessingStats
) : TextureTask {
    val coroutine by lazy {
        val typeName = this::class.simpleName ?: "[unnamed AbstractTextureTask]"
        scope.async(start = CoroutineStart.LAZY) {
            stats.onTaskLaunched(typeName, name)
            createImage().also {
                stats.onTaskCompleted(typeName, name)
            }
        }
    }
    val name by lazy { StringBuilder().also { formatTo(it) }.toString() }

    override fun isComplete() = coroutine.isCompleted
    override fun isStarted(): Boolean = coroutine.isActive || coroutine.isCompleted

    override suspend fun getImage(): PackedImage = coroutine.await()

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getImageNow(): PackedImage? = if (coroutine.isCompleted) coroutine.getCompleted() else null

    /** Must be final to supersede the generated implementation for data classes */
    final override fun toString(): String = name
    protected abstract suspend fun createImage(): PackedImage
    class JfxTask<T>(private val jfxCode: () -> T) : Task<T>() {
        override fun call(): T {
            val out = jfxCode()
            updateValue(out)
            return out
        }
    }
}

@Suppress("BlockingMethodInNonBlockingContext")
suspend fun <T> doJfx(name: String, retryer: Retryer, jfxCode: () -> T): T
        = retryer.retrying(name) {
    val task = AbstractTextureTask.JfxTask {jfxCode()}
    Platform.runLater(task)
    return@retrying withContext(Dispatchers.IO) {task.get()}
}
