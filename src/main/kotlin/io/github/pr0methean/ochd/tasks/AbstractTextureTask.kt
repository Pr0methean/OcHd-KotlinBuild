package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.Retryer
import io.github.pr0methean.ochd.packedimage.ImagePacker
import io.github.pr0methean.ochd.packedimage.PackedImage
import javafx.concurrent.Task
import javafx.scene.image.Image
import kotlinx.coroutines.*
import java.lang.Runnable
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles

// JavaFX insists on having only one "UI" thread that can do any image processing, which is stored in a static variable.
// To fool it into using more than one UI thread in parallel, we need more than one copy of the static variable, hence
// this ugly but effective workaround.
val threadLocalClassLoader: ThreadLocal<ClassLoader> = ThreadLocal.withInitial {MyClassLoader()}
private val threadLocalRunLater: ThreadLocal<MethodHandle> = ThreadLocal.withInitial {
    val classLoader = threadLocalClassLoader.get()
    classLoader.loadClass("javafx.embed.swing.JFXPanel").getConstructor().newInstance()
    val platformClass = classLoader.loadClass("javafx.application.Platform")
    val lookup = MethodHandles.lookup()
    lookup.unreflect(platformClass.getMethod("runLater", Runnable::class.java))
}

suspend fun <T> doJfx(name: String, retryer: Retryer, jfxCode: suspend CoroutineScope.() -> T): T
        = retryer.retrying(name) {
    val task = AbstractTextureTask.JfxTask(jfxCode)
    threadLocalRunLater.get().invokeExact(task as Runnable)
    return@retrying withContext(Dispatchers.IO) { task.get() }
}

abstract class AbstractTextureTask(
    open val packer: ImagePacker, open val scope: CoroutineScope, open val stats: ImageProcessingStats,
    open val retryer: Retryer) : TextureTask {
    open val coroutine by lazy {
        runTaskAsync(scope)
    }

    override fun isComplete() = coroutine.isCompleted

    override fun isStarted(): Boolean = coroutine.isActive || coroutine.isCompleted

    protected fun runTaskAsync(coroutineScope: CoroutineScope)
            = coroutineScope
                .plus(threadLocalClassLoader.asContextElement())
                .plus(threadLocalRunLater.asContextElement())
                .async(start = CoroutineStart.LAZY) {
        stats.onTaskLaunched(this@AbstractTextureTask)
        val name = this@AbstractTextureTask.toString()
        val image = retryer.retrying(name, ::computeImage)
        val result = retryer.retrying(name) {
            packer.packImage(image, null, this@AbstractTextureTask, name)
        }
        stats.onTaskCompleted(this@AbstractTextureTask)
        result
    }

    protected suspend fun <T> doJfx(jfxCode: suspend CoroutineScope.() -> T): T
            = doJfx(toString(), retryer, jfxCode)

    class JfxTask<T>(private val jfxCode: suspend CoroutineScope.() -> T) : Task<T>() {
        override fun call(): T {
            val out = runBlocking(Dispatchers.Unconfined, jfxCode)
            updateValue(out)
            return out
        }
    }

    /** Must be final to supercede the generated implementation for data classes */
    final override fun toString(): String = StringBuilder().also { formatTo(it) }.toString()

    abstract suspend fun computeImage(): Image
    override suspend fun getImage(): PackedImage = coroutine.await()

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getImageNow(): PackedImage? = if (coroutine.isCompleted) coroutine.getCompleted() else null
}