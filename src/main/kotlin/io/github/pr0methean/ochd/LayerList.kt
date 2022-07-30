package io.github.pr0methean.ochd

import io.github.pr0methean.ochd.tasks.consumable.ConsumableImageTask
import javafx.scene.paint.Color.TRANSPARENT
import javafx.scene.paint.Paint
import org.apache.logging.log4j.util.StringBuilderFormattable

data class LayerList(val layers: List<ConsumableImageTask>, val background: Paint):
        StringBuilderFormattable {
    override fun toString(): String = StringBuilder().also {formatTo(it)}.toString()
    override fun formatTo(buffer: StringBuilder) {
        if (background != TRANSPARENT) {
            buffer.append(background).append(", ")
        }
        buffer.appendList(layers)
    }
}