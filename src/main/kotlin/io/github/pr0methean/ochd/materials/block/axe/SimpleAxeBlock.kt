package io.github.pr0methean.ochd.materials.block.axe

import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.TaskPlanningContext
import io.github.pr0methean.ochd.tasks.OutputTask
import io.github.pr0methean.ochd.texturebase.Block
import io.github.pr0methean.ochd.texturebase.ShadowHighlightMaterial
import io.github.pr0methean.ochd.texturebase.SingleTextureMaterial
import javafx.scene.paint.Color
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

@Suppress("unused")
enum class SimpleAxeBlock: SingleTextureMaterial, ShadowHighlightMaterial, Block {
    CRAFTING_TABLE_SIDE {
        override suspend fun LayerListBuilder.createTextureLayers() {
            copy {OverworldWood.OAK.run {planks()}}
            layer("borderSolid", highlight)
            layer("craftingSide", OverworldWood.DARK_OAK.color)
        }

        override suspend fun outputTasks(ctx: TaskPlanningContext): Flow<OutputTask> = flow {
            val layers = ctx.stack {createTextureLayers()}
            emit(ctx.out(layers, "block/crafting_table_side", "block/crafting_table_front"))
        }
    },
    CRAFTING_TABLE_TOP {
        override suspend fun LayerListBuilder.createTextureLayers() {
            background(color)
            layer("waves", highlight)
            layer("commandBlockSquare", shadow)
            layer("craftingGridSpaces", color)
            layer("borderSolid", OverworldWood.DARK_OAK.color)
            layer("cornersTri", highlight)
        }
    },
    LADDER {
        override suspend fun LayerListBuilder.createTextureLayers() {
            layer("rail", color)
            layer("railTies", highlight)
        }
    },
    BOOKSHELF {
        override suspend fun LayerListBuilder.createTextureLayers() {
            background(color)
            layer("bookShelves")
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
    JUKEBOX_SIDE {
        override suspend fun LayerListBuilder.createTextureLayers() {
            background(color)
            layer("strokeTopLeftBottomRight4", highlight)
            layer("strokeBottomLeftTopRight4", shadow)
            layer("borderSolidThick", color)
            layer("borderDotted", shadow)
        }
    },
    NOTE_BLOCK {
        override suspend fun LayerListBuilder.createTextureLayers() {
            copy(JUKEBOX_SIDE)
            layer("note", OverworldWood.DARK_OAK.shadow)
        }
    },
    // Compost textures are part of DirtGroundCover.PODZOL
    COMPOSTER_BOTTOM {
        override suspend fun LayerListBuilder.createTextureLayers() {
            background(shadow)
            layer("planksTopVertical", color)
            layer("borderSolidThick", shadow)
            layer("borderSolid", color)
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
    };
    override val color = OverworldWood.OAK.color
    override val shadow = OverworldWood.OAK.shadow
    override val highlight = OverworldWood.OAK.highlight
}