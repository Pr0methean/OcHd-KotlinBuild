package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.ImageProcessingContext
import io.github.pr0methean.ochd.packedimage.PackedImage
import javafx.concurrent.Task
import javafx.scene.image.Image
import kotlinx.coroutines.*
import java.lang.Runnable
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.util.concurrent.ExecutionException

// JavaFX insists on having only one "UI" thread that can do any image processing, which is stored in a static variable.
// To fool it into using more than one UI thread in parallel, we need more than one copy of the static variable, hence
// this ugly but effective workaround.
private val platformClass: ThreadLocal<Class<*>> = ThreadLocal.withInitial {
    val classLoader = MyClassLoader()
    classLoader.loadClass("javafx.embed.swing.JFXPanel").getConstructor().newInstance()
    return@withInitial classLoader.loadClass("javafx.application.Platform")
}
private val threadLocalRunLater: ThreadLocal<MethodHandle> = ThreadLocal.withInitial {
    val lookup = MethodHandles.lookup()
    lookup.unreflect(platformClass.get().getMethod("runLater", Runnable::class.java))
}
abstract class TextureTask(open val ctx: ImageProcessingContext) {
    private val coroutine by lazy {
        ctx.scope.async(start = CoroutineStart.LAZY) {
            ctx.taskLaunches.add(this@TextureTask::class.simpleName ?: "[unnamed TextureTask subclass]")
            println("Starting task ${this@TextureTask}")
            var bitmap: Image? = null
            var attempt = 1L
            while (bitmap == null) {
                try {
                    bitmap = computeImage()
                } catch (t: Throwable) {
                    if (!ctx.shouldRetry(t, attempt)) {
                        throw ExecutionException(t)
                    }
                }
            }
            println("Finished task ${this@TextureTask}")
            return@async ctx.packImage(bitmap!!, this@TextureTask)
        }
    }

    protected suspend fun <T> doJfx(jfxCode: suspend CoroutineScope.() -> T): T {
        val task = JfxTask(jfxCode)
        var out: T? = null
        var attempts = 1L
        while (out == null) {
            try {
                threadLocalRunLater.get().invokeExact(task as Runnable)
                out = withContext(Dispatchers.IO) { task.get() }
            } catch (t: Throwable) {
                if (!ctx.shouldRetry(t, attempts)) {
                    throw ExecutionException(t)
                } else {
                    attempts++
                }
            }
        }
        return out
    }

    class JfxTask<T>(private val jfxCode: suspend CoroutineScope.() -> T) : Task<T>() {
        override fun call(): T {
            val out = runBlocking(Dispatchers.Unconfined, jfxCode)
            updateValue(out)
            return out
        }
    }

    abstract suspend fun computeImage(): Image
    suspend fun getImage(): PackedImage = coroutine.await()
}