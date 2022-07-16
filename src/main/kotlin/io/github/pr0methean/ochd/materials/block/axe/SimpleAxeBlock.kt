package io.github.pr0methean.ochd.materials.block.axe

import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.materials.block.shovel.DirtGroundCover
import io.github.pr0methean.ochd.texturebase.Block
import io.github.pr0methean.ochd.texturebase.ShadowHighlightMaterial
import io.github.pr0methean.ochd.texturebase.SingleTextureMaterial
import io.github.pr0methean.ochd.texturebase.group
import javafx.scene.paint.Color

val SIMPLE_AXE_BLOCKS = group<SimpleAxeBlock>()
@Suppress("unused")
enum class SimpleAxeBlock: SingleTextureMaterial, ShadowHighlightMaterial, Block {
    /*
out_layer borderSolidThick ${wood_oak} "block/composter_top"

push stripesThick ${wood_oak_s} compostSide0 ${wood_oak}
push borderDotted ${wood_oak_h} compostSide1
out_stack "block/composter_side"

push planksTopVertical ${wood_oak} compostBottom0 ${wood_oak_s}
push borderSolidThick ${wood_oak_s} compostBottom1
push borderSolid ${wood_oak} compostBottom2
out_stack "block/composter_bottom"
     */
    LADDER {
        override fun LayerListBuilder.createTextureLayers() {
            layer("rail", color)
            layer("railTies", highlight)
        }
    },
    CRAFTING_TABLE_TOP {
        override fun LayerListBuilder.createTextureLayers() {
            background(color)
            layer("waves", highlight)
            layer("craftingGrid", shadow)
            layer("borderSolid", OverworldWood.DARK_OAK.color)
            layer("cornersTri", highlight)
        }
    },
    CRAFTING_TABLE_SIDE {
        override fun LayerListBuilder.createTextureLayers() {
            OverworldWood.OAK.run {planks()}
            layer("borderSolid", highlight)
            layer("craftingSide", OverworldWood.DARK_OAK.color)
        }
    },
    CRAFTING_TABLE_FRONT {
        override fun LayerListBuilder.createTextureLayers() {
            copy(CRAFTING_TABLE_SIDE)
        }
    },
    BOOKSHELF {
        override fun LayerListBuilder.createTextureLayers() {
            background(color)
            layer("bookShelves")
        }
    },
    JUKEBOX_SIDE {
        override fun LayerListBuilder.createTextureLayers() {
            background(OverworldWood.DARK_OAK.color)
            layer("strokeTopLeftBottomRight4", color)
            layer("strokeBottomLeftTopRight4", shadow)
            layer("borderSolidThick", highlight)
            layer("borderDotted", shadow)
        }
    },
    JUKEBOX_TOP {
        override fun LayerListBuilder.createTextureLayers() {
            background(color)
            layer("borderSolidThick", highlight)
            layer("borderDotted", shadow)
            layer("thirdRail", Color.BLACK)
        }
    },
    NOTE_BLOCK {
        override fun LayerListBuilder.createTextureLayers() {
            copy(JUKEBOX_SIDE)
            layer("note", shadow)
        }
    },
    COMPOSTER_TOP {
        override fun LayerListBuilder.createTextureLayers() {
            layer("borderSolidThick", color)
        }
    },
    COMPOSTER_SIDE {
        override fun LayerListBuilder.createTextureLayers() {
            background(color)
            layer("stripesThick", shadow)
            layer("borderDotted", highlight)
        }
    },
    COMPOSTER_BOTTOM {
        override fun LayerListBuilder.createTextureLayers() {
            background(shadow)
            layer("planksTopVertical", color)
            layer("borderSolidThick", shadow)
            layer("borderSolid", color)
        }
    },
    COMPOSTER_COMPOST {
        override fun LayerListBuilder.createTextureLayers() = DirtGroundCover.PODZOL.run {createTopLayers()}
    },
    COMPOSTER_READY {
        override fun LayerListBuilder.createTextureLayers() {
            copy(COMPOSTER_COMPOST)
            layer("bonemealSmallNoBorder")
        }
    }
    /*

push_copy block/jukebox_side noteblock1
push note ${wood_oak_s} noteblock4
out_stack "block/note_block"
     */
    ;
    override val color = OverworldWood.OAK.color
    override val shadow = OverworldWood.OAK.shadow
    override val highlight = OverworldWood.OAK.highlight
}