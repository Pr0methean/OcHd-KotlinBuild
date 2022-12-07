package io.github.pr0methean.ochd.tasks

import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.Image
import org.apache.logging.log4j.util.StringBuilderFormattable


interface ImageTask : StringBuilderFormattable, Task<Image> {
    val asPng: TransformingTask<Image, ByteArray>
    override suspend fun mergeWithDuplicate(other: Task<*>): ImageTask

    fun opaqueRepaints(): Iterable<ImageTask>

    fun addOpaqueRepaint(repaint: ImageTask)

    suspend fun renderOnto(context: GraphicsContext, x: Double, y: Double)
}
