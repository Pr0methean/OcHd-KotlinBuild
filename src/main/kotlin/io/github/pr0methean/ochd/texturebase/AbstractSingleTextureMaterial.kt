package io.github.pr0methean.ochd.texturebase

import io.github.pr0methean.ochd.LayerList
import javafx.scene.paint.Color

abstract class AbstractSingleTextureMaterial(
    override val name: String,
    override val color: Color,
    override val shadow: Color,
    override val highlight: Color,
    override val nameOverride: String? = null,
    override val getTextureLayers: LayerList.() -> Unit

): ShadowHighlightMaterial, SingleTextureMaterial