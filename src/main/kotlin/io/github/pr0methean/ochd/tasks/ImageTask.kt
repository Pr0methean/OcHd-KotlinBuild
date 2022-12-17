package io.github.pr0methean.ochd.tasks

import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.Image
import org.apache.logging.log4j.util.StringBuilderFormattable


/** Specialization of [Task]&lt;[Image]&gt;. */
interface ImageTask : StringBuilderFormattable, Task<Image> {
    override suspend fun mergeWithDuplicate(other: Task<*>): ImageTask

    suspend fun renderOnto(context: GraphicsContext, x: Double, y: Double)
}
