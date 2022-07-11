package io.github.pr0methean.ochd.materials.block.barehand

import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.materials.block.shovel.DirtGroundCover
import io.github.pr0methean.ochd.texturebase.Block
import io.github.pr0methean.ochd.texturebase.ShadowHighlightMaterial
import io.github.pr0methean.ochd.texturebase.group
import javafx.scene.paint.Paint

val BIOME_COLORIZED_PLANTS = group<BiomeColorizedPlant>()
/**
 * Plants whose textures are stored in gray and colorized in real time.
 */
enum class BiomeColorizedPlant: Block, ShadowHighlightMaterial {
    /*
    # Lily pads and leaves are biome-colored starting from gray, like grass blocks

push lilyPad ${grass_s} pad1
push lilyPadInterior ${grass_h} pad2
out_stack block/lily_pad

# Protruding grass

push bottomPart ${grass_s} tallgrassb0
push grassTall ${grass} tallgrassb1
out_stack block/tall_grass_bottom

out_layer grassVeryShort ${grass} block/tall_grass_top

out_layer grassShort ${grass} block/grass
     */
    LILY_PAD {
        override fun LayerListBuilder.createTextureLayers() {
            layer("lilyPad", shadow)
            layer("lilyPadInterior", highlight)
        }
    },
    TALL_GRASS {
        override fun LayerListBuilder.createTextureLayers() {
            layer("bottomPart", shadow)
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

    override val color: Paint = DirtGroundCover.GRASS.color
    override val shadow: Paint = DirtGroundCover.GRASS.shadow
    override val highlight: Paint = DirtGroundCover.GRASS.highlight
}