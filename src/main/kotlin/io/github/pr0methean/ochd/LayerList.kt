package io.github.pr0methean.ochd

import io.github.pr0methean.ochd.tasks.TextureTask
import javafx.scene.paint.Color.TRANSPARENT
import javafx.scene.paint.Paint
import org.apache.logging.log4j.util.StringBuilderFormattable
import kotlin.text.StringBuilder

data class LayerList(val layers: List<TextureTask>, val background: Paint):
        StringBuilderFormattable {
    override fun toString(): String = StringBuilder().also {formatTo(it)}.toString()
    override fun formatTo(buffer: StringBuilder) {
        if (background != TRANSPARENT) {
            buffer.append(background).append(", ")
        }
        buffer.appendList(layers)
    }
}