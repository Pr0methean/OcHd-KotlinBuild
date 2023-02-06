package io.github.pr0methean.ochd

import javafx.scene.SnapshotParameters
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import org.apache.logging.log4j.util.StringBuilderFormattable

@Suppress("MagicNumber")
fun c(value: Int): Color = Color.rgb(
    value.shr(16).and(0xff),
    value.shr(8).and(0xff),
    value.and(0xff))

val DEFAULT_SNAPSHOT_PARAMS: SnapshotParameters = SnapshotParameters().also {
    it.fill = Color.TRANSPARENT
}

/**
 * A list is a shallow copy of another if they are equal and also refer to the same copies of their contents. This means
 * that having both on the heap won't cause their current contents to be on the heap more than once, which is useful
 * when T is [io.github.pr0methean.ochd.tasks.AbstractImageTask] because it can keep uncompressed 32-bit images alive.
 */
fun <T> List<T>.isShallowCopyOf(other: List<T>): Boolean {
    return this === other || (size == other.size && indices.all { this[it] === other[it] })
}

operator fun Paint.times(alpha: Double): Paint = if (alpha == 1.0) {
    this
} else if (this is Color) {
    Color(red, green, blue, opacity * alpha)
} else error("Alpha multiplier not implemented for non-Color paints")

fun StringBuilder.appendCollection(
        collection: Collection<StringBuilderFormattable>, delim: String = ", "): StringBuilder {
    if (collection.isNotEmpty()) {
        for (item in collection) {
            item.formatTo(this)
            append(delim)
        }
        delete(length - delim.length, length)
    }
    return this
}

