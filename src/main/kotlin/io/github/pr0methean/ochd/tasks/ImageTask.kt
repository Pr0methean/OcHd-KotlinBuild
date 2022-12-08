package io.github.pr0methean.ochd.tasks

import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.Image
import org.apache.logging.log4j.util.StringBuilderFormattable


/** Specialization of [Task]&lt;[Image]&gt;. */
interface ImageTask : StringBuilderFormattable, Task<Image> {
    val asPng: TransformingTask<Image, ByteArray>
    override suspend fun mergeWithDuplicate(other: Task<*>): ImageTask

    /**
     * These can be used by [RepaintTask] to repaint a repaint if it's cached or in progress and the original isn't.
     */
    fun opaqueRepaints(): Iterable<ImageTask>

    fun addOpaqueRepaint(repaint: ImageTask)

    suspend fun renderOnto(context: GraphicsContext, x: Double, y: Double)
}
