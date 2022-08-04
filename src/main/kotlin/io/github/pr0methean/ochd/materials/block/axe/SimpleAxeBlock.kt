package io.github.pr0methean.ochd.materials.block.axe

import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.materials.block.shovel.DirtGroundCover
import io.github.pr0methean.ochd.texturebase.Block
import io.github.pr0methean.ochd.texturebase.ShadowHighlightMaterial
import io.github.pr0methean.ochd.texturebase.SingleTextureMaterial
import javafx.scene.paint.Color

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
        override suspend fun LayerListBuilder.createTextureLayers() {
            layer("rail", color)
            layer("railTies", highlight)
        }
    },
    CRAFTING_TABLE_TOP {
        override suspend fun LayerListBuilder.createTextureLayers() {
            background(color)
            layer("waves", highlight)
            layer("craftingGrid", shadow)
            layer("borderSolid", OverworldWood.DARK_OAK.color)
            layer("cornersTri", highlight)
        }
    },
    CRAFTING_TABLE_SIDE {
        override suspend fun LayerListBuilder.createTextureLayers() {
            OverworldWood.OAK.run {planks()}
            layer("borderSolid", highlight)
            layer("craftingSide", OverworldWood.DARK_OAK.color)
        }
    },
    CRAFTING_TABLE_FRONT {
        override suspend fun LayerListBuilder.createTextureLayers() {
            copy(CRAFTING_TABLE_SIDE)
        }
    },
    BOOKSHELF {
        override suspend fun LayerListBuilder.createTextureLayers() {
            background(color)
            layer("bookShelves")
        }
    },
    JUKEBOX_SIDE {
        override suspend fun LayerListBuilder.createTextureLayers() {
            background(color)
            layer("strokeTopLeftBottomRight4", highlight)
            layer("strokeBottomLeftTopRight4", shadow)
            layer("borderSolidThick", color)
            layer("borderDotted", shadow)
        }
    },
    JUKEBOX_TOP {
        override suspend fun LayerListBuilder.createTextureLayers() {
            background(color)
            layer("borderSolidThick", highlight)
            layer("borderDotted", shadow)
            layer("thirdRail", Color.BLACK)
        }
    },
    NOTE_BLOCK {
        override suspend fun LayerListBuilder.createTextureLayers() {
            copy(JUKEBOX_SIDE)
            layer("note", OverworldWood.DARK_OAK.shadow)
        }
    },
    COMPOSTER_TOP {
        override suspend fun LayerListBuilder.createTextureLayers() {
            layer("borderSolidThick", color)
        }
    },
    COMPOSTER_SIDE {
        override suspend fun LayerListBuilder.createTextureLayers() {
            background(color)
            layer("stripesThick", shadow)
            layer("borderDotted", highlight)
        }
    },
    COMPOSTER_BOTTOM {
        override suspend fun LayerListBuilder.createTextureLayers() {
            background(shadow)
            layer("planksTopVertical", color)
            layer("borderSolidThick", shadow)
            layer("borderSolid", color)
        }
    },
    COMPOSTER_COMPOST {
        override suspend fun LayerListBuilder.createTextureLayers() {
            copy {DirtGroundCover.PODZOL.run {createTopLayers()}}
        }
    },
    COMPOSTER_READY {
        override suspend fun LayerListBuilder.createTextureLayers() {
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