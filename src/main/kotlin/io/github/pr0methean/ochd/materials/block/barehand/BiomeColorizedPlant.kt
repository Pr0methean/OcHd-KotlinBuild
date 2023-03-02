package io.github.pr0methean.ochd.materials.block.barehand

import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.materials.block.shovel.DirtGroundCover
import io.github.pr0methean.ochd.texturebase.Block
import io.github.pr0methean.ochd.texturebase.ShadowHighlightMaterial
import javafx.scene.paint.Paint

/**
 * Plants whose textures are stored in gray and colorized in real time.
 */
@Suppress("unused")
enum class BiomeColorizedPlant(override val hasOutput: Boolean = true): Block, ShadowHighlightMaterial {
    LILY_PAD {
        override fun LayerListBuilder.createTextureLayers() {
            layer("lilyPad", shadow)
            layer("lilyPadInterior", highlight)
        }
    },
    TALL_GRASS {
        override fun LayerListBuilder.createTextureLayers() {
            layer("bottomHalf", shadow)
            layer("grassTall", color)
        }
    },
    TALL_GRASS_TOP {
        override fun LayerListBuilder.createTextureLayers() {
            layer("grassVeryShort", color)
        }
    },
    GRASS {
        override fun LayerListBuilder.createTextureLayers() {
            layer("grassShort", color)
        }
    };

    override val color: Paint = DirtGroundCover.GRASS_BLOCK.color
    override val shadow: Paint = DirtGroundCover.GRASS_BLOCK.shadow
    override val highlight: Paint = DirtGroundCover.GRASS_BLOCK.highlight
}
