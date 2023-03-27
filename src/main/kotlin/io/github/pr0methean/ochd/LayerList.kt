package io.github.pr0methean.ochd

import io.github.pr0methean.ochd.tasks.AbstractImageTask
import javafx.scene.paint.Color.TRANSPARENT
import javafx.scene.paint.Paint
import org.apache.logging.log4j.util.StringBuilderFormattable

data class LayerList(val layers: List<AbstractImageTask>, val background: Paint, val width: Int, val height: Int):
        StringBuilderFormattable {
    private val toString by lazy { buildString {formatTo(this)} }
    override fun toString(): String = toString
    override fun formatTo(buffer: StringBuilder) {
        if (background != TRANSPARENT) {
            buffer.append(background).append(", ")
        }
        buffer.appendFormattables(layers)
    }

    fun mergeWithDuplicate(other: LayerList): LayerList {
        if (!layers.isShallowCopyOf(other.layers)) {
            val mergedLayers = layers.zip(other.layers).map { (a, b) -> if (a === b) a else a.mergeWithDuplicate(b) }
            return copy(layers = mergedLayers)
        }
        return this
    }
}
