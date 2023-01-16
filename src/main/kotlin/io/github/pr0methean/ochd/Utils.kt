package io.github.pr0methean.ochd

import javafx.scene.SnapshotParameters
import javafx.scene.paint.Color
import org.apache.logging.log4j.util.StringBuilderFormattable

@Suppress("MagicNumber")
fun c(value: Int): Color = Color.rgb(
    value.shr(16).and(0xff),
    value.shr(8).and(0xff),
    value.and(0xff))

val DEFAULT_SNAPSHOT_PARAMS: SnapshotParameters = SnapshotParameters().also {
    it.fill = Color.TRANSPARENT
}

fun <T> List<T>.isShallowCopyOf(other: List<T>): Boolean {
    return this === other || (size == other.size && indices.all { this[it] === other[it] })
}

fun StringBuilder.appendList(list: List<StringBuilderFormattable>, delim: String = ", "): StringBuilder {
    for (item in list) {
        item.formatTo(this)
        append(delim)
    }
    delete(length - delim.length, length)
    return this
}

