package io.github.pr0methean.ochd

import io.github.pr0methean.ochd.tasks.AbstractImageTask
import javafx.scene.paint.Color.TRANSPARENT
import javafx.scene.paint.Paint
import org.apache.logging.log4j.util.StringBuilderFormattable

data class LayerList(val layers: List<AbstractImageTask>, val background: Paint):
        StringBuilderFormattable {
    private val toString by lazy {StringBuilder().also {formatTo(it)}.toString()}
    override fun toString(): String = toString
    override fun formatTo(buffer: StringBuilder) {
        if (background != TRANSPARENT) {
            buffer.append(background).append(", ")
        }
        buffer.appendList(layers)
    }

    fun mergeWithDuplicate(other: LayerList): LayerList {
        if (layers !== other.layers) {
            val mergedLayers = layers.zip(other.layers).map { (a, b) -> if (a === b) a else a.mergeWithDuplicate(b) }
            return copy(layers = mergedLayers)
        }
        return this
    }
}
