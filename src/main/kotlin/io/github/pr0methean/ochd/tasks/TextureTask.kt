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

// JavaFX insists on having only one "UI" thread that can do any image processing, which is stored in a static variable.
// To fool it into using more than one UI thread in parallel, we need more than one copy of the static variable, hence
// this ugly but effective workaround.
val threadLocalClassLoader: ThreadLocal<MyClassLoader> = ThreadLocal.withInitial {
    val classLoader = MyClassLoader()
    classLoader.loadClass("javafx.embed.swing.JFXPanel").getConstructor().newInstance()
    return@withInitial classLoader
}
val threadLocalRunLater: ThreadLocal<MethodHandle> = ThreadLocal.withInitial {
    return@withInitial MethodHandles.lookup().unreflect(
        threadLocalClassLoader.get().loadClass("javafx.application.Platform").getMethod("runLater", Runnable::class.java))
}
private val OUT_OF_MEMORY_DELAY = 5.seconds
private val TASK_POLL_INTERVAL = 10.milliseconds

abstract class TextureTask<TJfxInput>(open val ctx: ImageProcessingContext) {
    private val coroutine by lazy {ctx.scope.async(start = CoroutineStart.LAZY) {
        println("Starting task ${this@TextureTask}")
        ctx.taskLaunches.add(this@TextureTask::class.simpleName ?: "[unnamed TextureTask subclass]")
        val bitmap = computeBitmap()
        println("Finished task ${this@TextureTask}")
        return@async ctx.packImage(bitmap)
    }}

    inner class JfxTask(val input: TJfxInput): Task<Image>() {
        override fun call(): Image {
            val out = doBlockingJfx(input)
            updateValue(out)
            return out
        }
    }

    suspend fun computeBitmap(): Image {
        val task = JfxTask(computeInput())
        val runLater: MethodHandle = threadLocalRunLater.get()
        return runLater.run{
            invokeExact(task as Runnable)
            withContext(Dispatchers.IO) {
                while (!task.isDone) {
                    delay(TASK_POLL_INTERVAL)
                }
                return@withContext task.get()
            }
        }
    }
    abstract suspend fun computeInput(): TJfxInput
    abstract fun doBlockingJfx(input: TJfxInput): Image
    suspend fun getPackedImage(): PackedImage = coroutine.await()
    suspend fun getImage(): Image = getPackedImage().unpack()
}