package io.github.pr0methean.ochd.texturebase

import javafx.scene.paint.Paint

open class SingleLayerItem(
    override val sourceFileName: String,
    override val nameOverride: String?,
    override val color: Paint? = null,
    override val alpha: Double = 1.0
) : SingleLayerMaterial("item", sourceFileName, nameOverride, color, alpha)