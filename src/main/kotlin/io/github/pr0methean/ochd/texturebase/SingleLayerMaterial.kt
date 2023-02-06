package io.github.pr0methean.ochd.texturebase

import io.github.pr0methean.ochd.LayerListBuilder
import javafx.scene.paint.Color
import javafx.scene.paint.Paint

abstract class SingleLayerMaterial(
    override val name: String,
    override val directory: String,
    private val sourceFileName: String,
    val color: Paint? = null,
    private val alpha: Double = 1.0
) : SingleTextureMaterial {
    init {
        if (color !is Color) {
            check(alpha == 1.0) { "Can't implement transparency" }
        }
    }
    override fun LayerListBuilder.createTextureLayers() {
        if (color != null) {
            layer(sourceFileName, color, alpha)
        } else {
            layer(sourceFileName)
        }
    }
}
