package io.github.pr0methean.ochd.tasks

import javafx.concurrent.Task
import javafx.scene.image.Image
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import java.lang.invoke.MethodHandles

class MyClassLoader(): ClassLoader()
val threadLocalClassLoader = ThreadLocal.withInitial {
    val classLoader = MyClassLoader()
    classLoader.loadClass("javafx.embed.swing.JFXPanel").getConstructor().newInstance()
    return@withInitial classLoader
}
val threadLocalRunLater = ThreadLocal.withInitial {
    return@withInitial MethodHandles.lookup().unreflect(
        threadLocalClassLoader.get().loadClass("javafx.application.Platform").getMethod("runLater", Runnable::class.java))
}

abstract class JfxTextureTask<TJfxInput>(open val scope: CoroutineScope) : TextureTask(scope) {
    inner class JfxTask(val input: TJfxInput): Task<Image>() {
        override fun call(): Image {
            val out = doBlockingJfx(input)
            updateValue(out)
            return out
        }

    }
    override suspend fun computeBitmap(): Image {
        val task = JfxTask(computeInput())
        threadLocalRunLater.get().invokeExact(task as Runnable)
        while (!task.isDone) {
            delay(10)
        }
        val image = task.get()
        return image!!
    }
    abstract suspend fun computeInput(): TJfxInput
    abstract fun doBlockingJfx(input: TJfxInput): Image
}