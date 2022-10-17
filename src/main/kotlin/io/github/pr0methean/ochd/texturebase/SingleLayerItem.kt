package io.github.pr0methean.ochd.texturebase

import javafx.scene.paint.Paint

open class SingleLayerItem(
    sourceFileName: String,
    name: String,
    color: Paint? = null,
    alpha: Double = 1.0
) : SingleLayerMaterial(name, "item", sourceFileName, color, alpha)