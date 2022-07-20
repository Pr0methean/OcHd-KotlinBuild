package io.github.pr0methean.ochd

import io.github.pr0methean.ochd.packedimage.PackedImage
import javafx.scene.paint.Color.TRANSPARENT
import javafx.scene.paint.Paint
import kotlinx.coroutines.Deferred
import org.apache.logging.log4j.util.StringBuilderFormattable

data class LayerList(val layers: List<Deferred<PackedImage>>, val background: Paint): StringBuilderFormattable {
    val name = StringBuilder().also(::formatTo).toString()
    override fun formatTo(buffer: StringBuilder) {
        if (background != TRANSPARENT) {
            buffer.append(background).append(',')
        }
        buffer.appendList(layers)
    }
    override fun toString() = name
}