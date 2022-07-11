package io.github.pr0methean.ochd.texturebase

import javafx.scene.paint.Paint

class SingleLayerParticle(
    override val sourceFileName: String,
    override val nameOverride: String?,
    override val color: Paint? = null,
    override val alpha: Double = 1.0):
    SingleLayerMaterial("particle", sourceFileName, nameOverride, color, alpha)