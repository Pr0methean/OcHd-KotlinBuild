package io.github.pr0methean.ochd.texturebase

import javafx.scene.paint.Paint

class SingleLayerParticle(
    sourceFileName: String,
    name: String,
    color: Paint? = null,
    alpha: Double = 1.0,
    hasOutput: Boolean = true
): SingleLayerMaterial(name, "particle", sourceFileName, color, alpha, hasOutput)
