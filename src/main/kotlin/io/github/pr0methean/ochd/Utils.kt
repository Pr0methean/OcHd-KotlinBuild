package io.github.pr0methean.ochd

import javafx.scene.SnapshotParameters
import javafx.scene.paint.Color
import org.apache.logging.log4j.util.StringBuilderFormattable

fun c(value: Int): Color = Color.rgb(
    value.shr(16).and(0xff),
    value.shr(8).and(0xff),
    value.and(0xff))

val DEFAULT_SNAPSHOT_PARAMS = SnapshotParameters().also {
    it.fill = Color.TRANSPARENT
}

fun StringBuilder.appendList(list: List<*>, delim: String = ", "): StringBuilder {
    if (list.isEmpty()) {
        return this
    }
    for (item in list) {
        if (item is StringBuilderFormattable) {
            item.formatTo(this)
        } else {
            append(item)
        }
        append(delim)
    }
    delete(length - delim.length, length)
    return this
}

