package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.ImageProcessingContext
import javafx.concurrent.Task
import javafx.scene.image.Image
import kotlinx.coroutines.delay
import java.lang.Thread.sleep
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.util.concurrent.ThreadLocalRandom
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

abstract class JfxTextureTask<TJfxInput>(ctx: ImageProcessingContext) : TextureTask(ctx) {
    inner class JfxTask(val input: TJfxInput): Task<Image>() {
        override fun call(): Image {
            val out = doBlockingJfx(input)
            updateValue(out)
            return out
        }
    }

    private fun getOutOfMemoryDelay() = (2500 + ThreadLocalRandom.current().nextInt(5000)).milliseconds
    protected suspend fun <T> retryOnOom(block: suspend () -> T): T {
        while (true) {
            try {
                return block()
            } catch (e: OutOfMemoryError) {
                val delay = getOutOfMemoryDelay()
                println("Delaying for $delay after OutOfMemoryError in $this")
                delay(delay)
            }
        }
    }
    protected fun <T> retryOnOomBlocking(block: () -> T): T {
        while (true) {
            try {
                return block()
            } catch (e: OutOfMemoryError) {
                val delay = getOutOfMemoryDelay()
                println("Sleeping for $delay after OutOfMemoryError in $this")
                sleep(delay.inWholeMilliseconds)
            }
        }
    }
    override suspend fun computeBitmap(): Image {
        val task = retryOnOom { JfxTask( retryOnOom(::computeInput)) }
        val runLater: MethodHandle = threadLocalRunLater.get()
        runLater.run{ retryOnOom<Unit> {invokeExact(task as Runnable)}}
        while (!task.isDone) {
            delay(TASK_POLL_INTERVAL)
        }
        val image = task.get()
        return image!!
    }
    abstract suspend fun computeInput(): TJfxInput
    abstract fun doBlockingJfx(input: TJfxInput): Image
}