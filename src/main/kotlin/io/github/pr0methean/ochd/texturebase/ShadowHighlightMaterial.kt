package io.github.pr0methean.ochd.texturebase

import javafx.scene.paint.Paint

interface ShadowHighlightMaterial: Material {
    val color: Paint
    val shadow: Paint
    val highlight: Paint
}
