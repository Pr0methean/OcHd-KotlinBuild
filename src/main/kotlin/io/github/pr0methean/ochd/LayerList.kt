package io.github.pr0methean.ochd

import io.github.pr0methean.ochd.tasks.consumable.ImageTask
import javafx.scene.paint.Color.TRANSPARENT
import javafx.scene.paint.Paint
import org.apache.logging.log4j.util.StringBuilderFormattable

data class LayerList(val layers: List<ImageTask>, val background: Paint):
        StringBuilderFormattable {
    override fun toString(): String = StringBuilder().also {formatTo(it)}.toString()
    override fun formatTo(buffer: StringBuilder) {
        if (background != TRANSPARENT) {
            buffer.append(background).append(", ")
        }
        buffer.appendList(layers)
    }

    suspend fun mergeWithDuplicate(other: LayerList) {
        if (background != other.background
            || layers.size != other.layers.size) {
            return
        }
        for ((index, layer) in layers.withIndex()) {
            layer.mergeWithDuplicate(other.layers[index])
        }
    }
}