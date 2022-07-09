package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.ImageProcessingContext
import io.github.pr0methean.ochd.packedimage.PackedImage
import javafx.concurrent.Task
import javafx.scene.image.Image
import kotlinx.coroutines.*
import java.lang.Runnable
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private val OUT_OF_MEMORY_DELAY = 5.seconds
private val TASK_POLL_INTERVAL = 10.milliseconds
private val threadLocalRunLater: ThreadLocal<MethodHandle> = ThreadLocal.withInitial {
    val classLoader = MyClassLoader()
    classLoader.loadClass("javafx.embed.swing.JFXPanel").getConstructor().newInstance()
    val platformClass = classLoader.loadClass("javafx.application.Platform")
    val lookup = MethodHandles.lookup()
    lookup.unreflect(platformClass.getMethod("runLater", Runnable::class.java))
}
private val onUiThread: ThreadLocal<Boolean> = ThreadLocal.withInitial {false}
abstract class TextureTask(open val ctx: ImageProcessingContext) {
    private val coroutine by lazy {
        ctx.scope.async(start = CoroutineStart.LAZY) {
            println("Starting task ${this@TextureTask}")
            ctx.taskLaunches.add(this@TextureTask::class.simpleName ?: "[unnamed TextureTask subclass]")
            val bitmap = computeImage()
            println("Finished task ${this@TextureTask}")
            return@async ctx.packImage(bitmap, this@TextureTask)
        }
    }
    // JavaFX insists on having only one "UI" thread that can do any image processing, which is stored in a static variable.
// To fool it into using more than one UI thread in parallel, we need more than one copy of the static variable, hence
// this ugly but effective workaround.

    protected suspend fun <T> doJfx(jfxCode: suspend CoroutineScope.() -> T): T {
        val task = JfxTask(jfxCode)
        threadLocalRunLater.get().invokeExact(task as Runnable)
        return withContext(Dispatchers.IO) {task.get()}
    }

    class JfxTask<T>(private val jfxCode: suspend CoroutineScope.() -> T) : Task<T>() {
        override fun call(): T {
            onUiThread.set(true)
            val out = runBlocking(Dispatchers.Unconfined, jfxCode)
            updateValue(out)
            return out
        }
    }

    abstract suspend fun computeImage(): Image
    suspend fun getImage(): PackedImage = coroutine.await()
}