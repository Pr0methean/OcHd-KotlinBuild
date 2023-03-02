package io.github.pr0methean.ochd.materials.block.shears

import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.materials.block.shovel.DirtGroundCover
import io.github.pr0methean.ochd.texturebase.Block
import io.github.pr0methean.ochd.texturebase.SingleTextureMaterial
import javafx.scene.paint.Color

@Suppress("unused")
enum class SimpleShearBlock(override val hasOutput: Boolean = true): SingleTextureMaterial, Block {
    COBWEB {
        override fun LayerListBuilder.createTextureLayers() {
            layer("ringsCentralBullseye", Color.WHITE, 0.75)
            layer("strokeBottomLeftTopRight", Color.WHITE, 0.85)
            layer("strokeTopLeftBottomRight", Color.WHITE, 0.85)
            layer("cross", Color.WHITE, 0.85)
        }
    },
    VINE {
        override fun LayerListBuilder.createTextureLayers() {
            layer("wavyVines", DirtGroundCover.GRASS_BLOCK.highlight)
            layer("waves", DirtGroundCover.GRASS_BLOCK.shadow)
        }
    };
}
