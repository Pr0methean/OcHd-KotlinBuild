package io.github.pr0methean.ochd.materials.item

import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.materials.block.pickaxe.SimplePickaxeBlock
import io.github.pr0methean.ochd.texturebase.Item
import io.github.pr0methean.ochd.texturebase.ShadowHighlightMaterial
import io.github.pr0methean.ochd.texturebase.SingleTextureMaterial
import javafx.scene.paint.Paint

@Suppress("unused")
enum class SingleTextureItem(
    override val color: Paint,
    override val shadow: Paint,
    override val highlight: Paint
): SingleTextureMaterial, ShadowHighlightMaterial, Item {
    AMETHYST_SHARD(SimplePickaxeBlock.AMETHYST_BLOCK) {
        override fun LayerListBuilder.createTextureLayers() {
            layer("trianglesSmallCenter1", highlight)
            layer("trianglesSmallCenter2", color)
        }
    };

    constructor(base: ShadowHighlightMaterial): this(base.color, base.shadow, base.highlight)
}
