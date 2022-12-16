package io.github.pr0methean.ochd.materials.block.pickaxe

import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.TaskPlanningContext
import io.github.pr0methean.ochd.c
import io.github.pr0methean.ochd.materials.block.pickaxe.OreBase.Companion.stoneExtremeHighlight
import io.github.pr0methean.ochd.materials.block.pickaxe.OreBase.Companion.stoneExtremeShadow
import io.github.pr0methean.ochd.materials.block.pickaxe.OreBase.DEEPSLATE
import io.github.pr0methean.ochd.materials.block.pickaxe.OreBase.STONE
import io.github.pr0methean.ochd.materials.block.shovel.SimpleSoftEarth
import io.github.pr0methean.ochd.tasks.FileOutputTask
import io.github.pr0methean.ochd.texturebase.Block
import io.github.pr0methean.ochd.texturebase.ShadowHighlightMaterial
import io.github.pr0methean.ochd.texturebase.SingleTextureMaterial
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

val mortarColor: Color = c(0xa2867d)
@Suppress("unused")
enum class SimplePickaxeBlock(
    override val color: Paint,
    override val shadow: Paint,
    override val highlight: Paint,
    private val hasOutput: Boolean = true
): SingleTextureMaterial, ShadowHighlightMaterial, Block {
    SMOOTH_STONE(STONE) {
        override suspend fun LayerListBuilder.createTextureLayers() {
            background(color)
            layer("borderSolid", stoneExtremeShadow)
        }
    },
    COBBLESTONE_BASE(STONE, false) {
        override suspend fun LayerListBuilder.createTextureLayers() {
            background(highlight)
            layer("checksLarge",shadow)
            layer("checksSmall",color)
        }
    },
    COBBLESTONE(STONE) {
        override suspend fun LayerListBuilder.createTextureLayers() {
            copy(COBBLESTONE_BASE)
            layer("borderSolid", stoneExtremeHighlight)
            layer("borderShortDashes", stoneExtremeShadow)
        }
    },
    MOSSY_COBBLESTONE(SimpleSoftEarth.MOSS_BLOCK) {
        override suspend fun LayerListBuilder.createTextureLayers() {
            copy(COBBLESTONE_BASE)
            layer("dots3",highlight)
            layer("dots2",shadow)
            layer("dots1",color)
            layer("borderSolid", highlight)
            layer("borderShortDashes", shadow)
        }
    },
    COBBLED_DEEPSLATE(DEEPSLATE) {
        override suspend fun LayerListBuilder.createTextureLayers() {
            background(shadow)
            layer("checksLarge", highlight)
            layer("checksSmall",color)
        }
    },
    SANDSTONE_BASE(SimpleSoftEarth.SAND, hasOutput = false) {
        override suspend fun LayerListBuilder.createTextureLayers() {
            background(color)
            layer("checksLarge", shadow)
        }
    },
    SANDSTONE_BOTTOM(SimpleSoftEarth.SAND) {
        override suspend fun LayerListBuilder.createTextureLayers() {
            copy(SANDSTONE_BASE)
            layer("borderLongDashes", highlight)
        }
    },
    SANDSTONE_TOP(SimpleSoftEarth.SAND) {
        override suspend fun LayerListBuilder.createTextureLayers() {
            background(color)
            layer("borderSolid", highlight)
            layer("checksLarge", shadow)
        }
    },
    SANDSTONE(SimpleSoftEarth.SAND) {
        override suspend fun LayerListBuilder.createTextureLayers() {
            background(color)
            layer("topPart", shadow)
            layer("borderSolid", shadow)
            layer("topStripeThick", highlight)
            layer("borderShortDashes", highlight)
        }
    },
    CUT_SANDSTONE(SimpleSoftEarth.SAND) {
        override suspend fun LayerListBuilder.createTextureLayers() {
            background(color)
            layer("checksLargeOutline", highlight)
            layer("borderSolid", shadow)
            layer("borderSolidTopLeft", highlight)
            layer("borderLongDashes", color)
        }
    },
    CHISELED_SANDSTONE(SimpleSoftEarth.SAND) {
        override suspend fun LayerListBuilder.createTextureLayers() {
            copy(CUT_SANDSTONE)
            layer("creeperFaceSmall", shadow)
        }
    },
    RED_SANDSTONE_BASE(SimpleSoftEarth.RED_SAND, hasOutput = false) {
        override suspend fun LayerListBuilder.createTextureLayers() {
            background(color)
            layer("borderSolid", highlight)
            layer("checksLarge", highlight)
            layer("checksLargeOutline", shadow)
        }
    },
    RED_SANDSTONE_BOTTOM(SimpleSoftEarth.RED_SAND) {
        override suspend fun LayerListBuilder.createTextureLayers() {
            copy(RED_SANDSTONE_BASE)
            layer("borderLongDashes", color)
        }
    },
    RED_SANDSTONE_TOP(SimpleSoftEarth.RED_SAND) {
        override suspend fun LayerListBuilder.createTextureLayers() {
            copy(RED_SANDSTONE_BASE)
            layer("borderSolidThick", highlight)
            layer("borderLongDashes", shadow)
        }
    },
    CUT_RED_SANDSTONE(SimpleSoftEarth.RED_SAND) {
        override suspend fun LayerListBuilder.createTextureLayers() {
            background(color)
            layer("borderSolid", shadow)
            layer("checksLarge", highlight)
            layer("borderSolidTopLeft", highlight)
        }
    },
    CHISELED_RED_SANDSTONE(SimpleSoftEarth.RED_SAND) {
        override suspend fun LayerListBuilder.createTextureLayers() {
            copy(CUT_RED_SANDSTONE)
            layer("witherSymbol", shadow)
        }
    },
    RED_SANDSTONE(SimpleSoftEarth.RED_SAND) {
        override suspend fun LayerListBuilder.createTextureLayers() {
            background(color)
            layer("topPart",shadow)
            layer("topStripeThick", highlight)
            layer("borderSolid", highlight)
            layer("borderShortDashes", shadow)
        }
    },
    BASALT_TOP(c(0x515151), c(0x003939), c(0x737373)) {
        override suspend fun LayerListBuilder.createTextureLayers() {
            background(color)
            layer("borderSolid", highlight)
            layer("borderLongDashes", shadow)
            layer("bigRingsBottomLeftTopRight", highlight)
            layer("bigRingsTopLeftBottomRight", shadow)
            layer("x", color)
            layer("bigDiamond", color)
        }
    },
    BASALT_SIDE(BASALT_TOP) {
        override suspend fun LayerListBuilder.createTextureLayers() {
            background(shadow)
            layer("stripesVerticalThick", color)
            layer("borderLongDashes", highlight)
        }
    },
    POLISHED_BASALT_TOP(BASALT_TOP) {
        override suspend fun LayerListBuilder.createTextureLayers() {
            background(color)
            layer("ringsCentralBullseye", shadow)
            layer("rings", highlight)
            layer("borderSolid", shadow)
            layer("borderSolidTopLeft", highlight)
            layer("cross", highlight)
            layer("crossDotted", shadow)
        }
    },
    POLISHED_BASALT_SIDE(BASALT_SIDE) {
        override suspend fun LayerListBuilder.createTextureLayers() {
            background(color)
            layer("stripesVerticalThick", highlight)
            layer("borderSolid", shadow)
            layer("borderSolidTopLeft", highlight)
        }
    },
    GLOWSTONE(c(0xcc8654), c(0x6f4522), c(0xffda74)) {
        override suspend fun LayerListBuilder.createTextureLayers() {
            background(color)
            layer("borderSolid", shadow)
            layer("checksSmall", highlight)
            layer("lampOn", Color.WHITE)
        }
    },
    END_STONE(c(0xdeffa4),c(0xc5be8b),c(0xffffb4)) {
        override suspend fun LayerListBuilder.createTextureLayers() {
            background(color)
            layer("checksLargeOutline", shadow)
            layer("bigDotsTopLeftBottomRight", shadow)
            layer("bigRingsTopLeftBottomRight", highlight)
        }
    },
    END_STONE_BRICKS(END_STONE) {
        override suspend fun LayerListBuilder.createTextureLayers() {
            background(highlight)
            layer("checksSmall", color)
            layer("bricksSmall", shadow)
            layer("borderShortDashes", highlight)
        }
    },
    QUARTZ_PILLAR(Ore.QUARTZ) {
        override suspend fun LayerListBuilder.createTextureLayers() {
            background(shadow)
            layer("tntSticksSide", color)
            layer("borderSolid", shadow)
            layer("borderDotted", highlight)
        }
    },
    QUARTZ_PILLAR_TOP(QUARTZ_PILLAR) {
        override suspend fun LayerListBuilder.createTextureLayers() {
            background(color)
            layer("rings", highlight)
            layer("borderSolid", shadow)
            layer("borderDotted", highlight)
        }
    },
    MUD_BRICKS(SimpleSoftEarth.PACKED_MUD) {
        override suspend fun LayerListBuilder.createTextureLayers() {
            background(color)
            layer("bricks", SimpleSoftEarth.MUD.shadow)
            layer("strokeTopLeftBottomRight2", highlight)
            layer("strokeBottomLeftTopRight2", shadow)
            layer("borderDotted", SimpleSoftEarth.MUD.highlight)
        }
    },
    STONE_BRICKS(STONE) {
        override suspend fun LayerListBuilder.createTextureLayers() {
            background(color)
            layer("checksLarge", highlight)
            layer("bricks", stoneExtremeShadow)
            layer("borderShortDashes", shadow)
        }
    },
    CRACKED_STONE_BRICKS(STONE_BRICKS) {
        override suspend fun LayerListBuilder.createTextureLayers() {
            copy(STONE_BRICKS)
            layer("streaks", stoneExtremeShadow)
        }
    },
    MOSSY_STONE_BRICKS(SimpleSoftEarth.MOSS_BLOCK) {
        override suspend fun LayerListBuilder.createTextureLayers() {
            background(STONE.color)
            layer("checksLarge", STONE.highlight)
            layer("bricks", stoneExtremeShadow)
            layer("dots3", shadow)
            layer("dots2", highlight)
            layer("dots1", color)
            layer("borderSolid", highlight)
            layer("borderShortDashes", shadow)
        }
    },
    CHISELED_STONE_BRICKS(STONE_BRICKS) {
        override suspend fun LayerListBuilder.createTextureLayers() {
            background(color)
            layer("ringsCentralBullseye", stoneExtremeHighlight)
            layer("rings2", stoneExtremeShadow)
            layer("borderSolid", stoneExtremeShadow)
            layer("borderSolidTopLeft", stoneExtremeHighlight)
        }
    },
    TERRACOTTA(c(0x945b43), c(0x945b43), c(0x9b6045)) {
        override suspend fun LayerListBuilder.createTextureLayers() {
            background(color)
            layer("bigDotsTopLeftBottomRight", shadow)
            layer("bigDotsBottomLeftTopRight", highlight)
            layer("bigRingsTopLeftBottomRight", highlight)
            layer("bigRingsBottomLeftTopRight", shadow)
            layer("borderRoundDots", highlight)
        }
    },
    BRICKS(mortarColor, TERRACOTTA.shadow,  mortarColor) {
        override suspend fun LayerListBuilder.createTextureLayers() {
            background(shadow)
            layer("bricksSmall", highlight)
            layer("borderDotted", highlight, 0.5)
        }
    },
    QUARTZ_BRICKS(Ore.QUARTZ) {
        override suspend fun LayerListBuilder.createTextureLayers() {
            background(color)
            layer("streaks", highlight)
            layer("bricks", shadow)
            layer("borderDotted", highlight)
        }
    },
    POLISHED_BLACKSTONE_BRICKS(Polishable.BLACKSTONE) {
        override suspend fun LayerListBuilder.createTextureLayers() {
            background(color)
            layer("bricksSmall", shadow)
            layer("borderDotted", highlight)
        }
    },
    NETHER_BRICKS(c(0x302020), Color.BLACK, c(0x442929)) {
        override suspend fun LayerListBuilder.createTextureLayers() {
            background(color)
            layer("bricksSmall", highlight)
            layer("borderDotted", highlight)
            layer("borderDottedBottomRight", shadow)
        }
    },
    RED_NETHER_BRICKS(c(0x440000),c(0x2e0000),c(0x730000)) {
        override suspend fun LayerListBuilder.createTextureLayers() {
            background(color)
            layer("bricksSmall", shadow)
            layer("borderDotted", highlight)
            layer("borderDottedBottomRight", shadow)
        }
    },
    AMETHYST_BLOCK(c(0xc890ff),c(0x7a5bb5),c(0xffcbff)) {
        override suspend fun LayerListBuilder.createTextureLayers() {
            background(shadow)
            layer("triangles1", highlight)
            layer("triangles2", color)
        }
    },
    BUDDING_AMETHYST(AMETHYST_BLOCK.color,c(0x462b7d),AMETHYST_BLOCK.highlight) {
        override suspend fun LayerListBuilder.createTextureLayers() {
            copy(AMETHYST_BLOCK)
            layer("buddingAmethystCenter", shadow)
        }
    },
    AMETHYST_CLUSTER(AMETHYST_BLOCK) {
        override suspend fun LayerListBuilder.createTextureLayers() {
            layer("amethystCluster1", highlight)
            layer("amethystCluster2", color)
        }
    },
    LARGE_AMETHYST_BUD(AMETHYST_BLOCK) {
        override suspend fun LayerListBuilder.createTextureLayers() {
            layer("largeAmethystBud1", color)
            layer("largeAmethystBud2", shadow)
            layer("largeAmethystBud3", highlight)
        }
    },
    MEDIUM_AMETHYST_BUD(AMETHYST_BLOCK) {
        override suspend fun LayerListBuilder.createTextureLayers() {
            layer("mediumAmethystBud1", color)
            layer("mediumAmethystBud2", shadow)
            layer("largeAmethystBud3", highlight)
        }
    },
    SMALL_AMETHYST_BUD(AMETHYST_BLOCK) {
        override suspend fun LayerListBuilder.createTextureLayers() {
            layer("smallAmethystBud1", color)
            layer("smallAmethystBud2", shadow)
        }
    }
    ;

    override suspend fun outputTasks(ctx: TaskPlanningContext): Flow<FileOutputTask> =
        if (hasOutput) super<SingleTextureMaterial>.outputTasks(ctx) else flowOf()

    constructor(base: ShadowHighlightMaterial, hasOutput: Boolean = true):
            this(base.color, base.shadow, base.highlight, hasOutput)
}
