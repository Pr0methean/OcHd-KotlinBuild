package io.github.pr0methean.ochd.texturebase

import javafx.scene.paint.Paint

class SingleLayerBlock(
    override val sourceFileName: String,
    override val nameOverride: String?,
    override val color: Paint? = null,
    override val alpha: Double = 1.0
) : AbstractSingleLayerMaterial("block", sourceFileName, nameOverride, color, alpha)