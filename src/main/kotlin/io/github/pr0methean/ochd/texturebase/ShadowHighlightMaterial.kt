package io.github.pr0methean.ochd.texturebase

import javafx.scene.paint.Color

interface ShadowHighlightMaterial: Material {
    val color: Color
    val shadow: Color
    val highlight: Color
}