package io.github.pr0methean.ochd

import io.github.pr0methean.ochd.tasks.TextureTask
import javafx.scene.paint.Color.TRANSPARENT
import javafx.scene.paint.Paint
import org.apache.logging.log4j.util.StringBuilderFormattable
import java.lang.StringBuilder

data class LayerList(val layers: List<TextureTask>, val background: Paint, val ctx: ImageProcessingContext):
        StringBuilderFormattable {
    override fun toString(): String = if (background == TRANSPARENT) "$layers" else "$background, $layers"
    override fun formatTo(buffer: StringBuilder) {
        if (background != TRANSPARENT) {
            buffer.append(background).append(", ")
        }
        buffer.appendList(layers)
    }
}