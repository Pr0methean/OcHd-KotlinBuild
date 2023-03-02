package io.github.pr0methean.ochd.materials.block.axe

import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.OutputTaskBuilder
import io.github.pr0methean.ochd.texturebase.Block
import io.github.pr0methean.ochd.texturebase.ShadowHighlightMaterial
import io.github.pr0methean.ochd.texturebase.SingleTextureMaterial
import javafx.scene.paint.Paint

@Suppress("unused")
enum class SimpleAxeBlock(override val hasOutput: Boolean = true): SingleTextureMaterial, ShadowHighlightMaterial, Block {
    CRAFTING_TABLE_SIDE {
        override fun LayerListBuilder.createTextureLayers() {
            copy {
                background(color)
                layer("waves2", highlight)
                layer("waves", shadow)
                layer("planksTopBorder", shadow)
            }
            layer("borderSolid", highlight)
            layer("craftingSide", OverworldWood.DARK_OAK.color)
        }

        override suspend fun OutputTaskBuilder.outputTasks() {
            out("block/crafting_table_side", "block/crafting_table_front") {
                createTextureLayers()
            }
        }
    },
    CRAFTING_TABLE_TOP {
        override fun LayerListBuilder.createTextureLayers() {
            background(color)
            layer("waves", highlight)
            layer("craftingGridSquare", shadow)
            layer("craftingGridSpaces", color)
            layer("borderSolid", OverworldWood.DARK_OAK.color)
            layer("cornersTri", highlight)
        }
    },
    LADDER {
        override fun LayerListBuilder.createTextureLayers() {
            layer("rail", color)
            layer("railTies", highlight)
        }
    },
    BOOKSHELF {
        override fun LayerListBuilder.createTextureLayers() {
            background(color)
            layer("bookShelves")
        }
    },
    JUKEBOX_TOP {
        override fun LayerListBuilder.createTextureLayers() {
            background(color)
            layer("borderSolidThick", highlight)
            layer("borderDotted", shadow)
            layer("thirdRail")
        }
    },
    JUKEBOX_SIDE {
        override fun LayerListBuilder.createTextureLayers() {
            background(highlight)
            layer("strokeTopLeftBottomRight4", shadow)
            layer("strokeBottomLeftTopRight4", shadow)
            layer("borderSolidThick", color)
            layer("borderSolid", highlight)
            layer("borderDotted", shadow)
        }
    },
    NOTE_BLOCK {
        override fun LayerListBuilder.createTextureLayers() {
            copy(JUKEBOX_SIDE)
            layer("note", OverworldWood.DARK_OAK.shadow)
        }
    },
    // Compost textures are part of DirtGroundCover.PODZOL
    COMPOSTER_BOTTOM {
        override fun LayerListBuilder.createTextureLayers() {
            background(shadow)
            layer("planksTopVertical", color)
            layer("borderSolidThick", shadow)
            layer("borderSolid", color)
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
    };
    override val color: Paint = OverworldWood.OAK.color
    override val shadow: Paint = OverworldWood.OAK.shadow
    override val highlight: Paint = OverworldWood.OAK.highlight
}
