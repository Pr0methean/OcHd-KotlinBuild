package io.github.pr0methean.ochd

import javafx.scene.paint.Color

fun c(value: Int): Color = Color.rgb(
    value.shr(16).and(0xff),
    value.shr(8).and(0xff),
    value.and(0xff))

