package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.ImageProcessingContext
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

abstract class TextureTask(open val ctx: ImageProcessingContext) {
    open val coroutine by lazy {
        asyncInScope(ctx.scope)
    }

    protected fun asyncInScope(coroutineScope: CoroutineScope) = coroutineScope.async(start = CoroutineStart.LAZY) {
        ctx.onTaskLaunched(this@TextureTask)
        val result = ctx.retrying { ctx.packImage(computeImage(), this@TextureTask, this@TextureTask.toString()) }
        ctx.onTaskCompleted(this@TextureTask)
        return@async result
    }

    protected suspend fun <T> doJfx(jfxCode: suspend CoroutineScope.() -> T): T = ctx.retrying {
        val task = JfxTask(jfxCode)
        threadLocalRunLater.get().invokeExact(task as Runnable)
        return@retrying withContext(Dispatchers.IO) { task.get() }
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