package io.github.pr0methean.ochd

import javafx.scene.SnapshotParameters
import javafx.scene.paint.Color
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.apache.logging.log4j.util.StringBuilderFormattable

fun c(value: Int): Color = Color.rgb(
    value.shr(16).and(0xff),
    value.shr(8).and(0xff),
    value.and(0xff))

val DEFAULT_SNAPSHOT_PARAMS = SnapshotParameters().also {
    it.fill = Color.TRANSPARENT
}

fun StringBuilder.appendList(list: List<StringBuilderFormattable>, delim: String = ", "): StringBuilder {
    for (item in list) {
        item.formatTo(this)
        append(delim)
    }
    delete(length - delim.length, length)
    return this
}

suspend fun <T> Semaphore?.withPermitIfNeeded(block: suspend () -> T): T = this?.withPermit { block() } ?: block()
