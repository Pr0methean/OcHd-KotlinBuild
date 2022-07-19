package io.github.pr0methean.ochd.materials.block.pickaxe

import io.github.pr0methean.ochd.ImageProcessingContext
import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.c
import io.github.pr0methean.ochd.materials.block.pickaxe.OreBase.Companion.stoneExtremeHighlight
import io.github.pr0methean.ochd.materials.block.pickaxe.OreBase.Companion.stoneExtremeShadow
import io.github.pr0methean.ochd.materials.block.pickaxe.OreBase.DEEPSLATE
import io.github.pr0methean.ochd.materials.block.pickaxe.OreBase.STONE
import io.github.pr0methean.ochd.materials.block.shovel.SimpleSoftEarth
import io.github.pr0methean.ochd.tasks.OutputTask
import io.github.pr0methean.ochd.texturebase.*
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

val SIMPLE_PICKAXE_BLOCKS = group<SimplePickaxeBlock>()
val mortarColor = c(0xa2867d)
@Suppress("unused")
enum class SimplePickaxeBlock(
    override val color: Paint,
    override val shadow: Paint,
    override val highlight: Paint,
    val hasOutput: Boolean = true
): SingleTextureMaterial, ShadowHighlightMaterial, Block {
    SMOOTH_STONE(STONE) {
        override fun LayerListBuilder.createTextureLayers() {
            background(color)
            layer("borderSolid", stoneExtremeShadow)
        }
    },
    COBBLESTONE(STONE) {
        override fun LayerListBuilder.createTextureLayers() {
            background(highlight)
            layer("checksLarge",shadow)
            layer("checksSmall",color)
            layer("borderSolid", stoneExtremeHighlight)
            layer("borderShortDashes", stoneExtremeShadow)
        }
    },
    MOSSY_COBBLESTONE(SimpleSoftEarth.MOSS_BLOCK) {
        override fun LayerListBuilder.createTextureLayers() {
            copy(COBBLESTONE)
            layer("dots3",highlight)
            layer("dots2",shadow)
            layer("dots1",color)
            layer("borderSolid", highlight)
            layer("borderShortDashes", shadow)
        }
    },
    COBBLED_DEEPSLATE(DEEPSLATE) {
        override fun LayerListBuilder.createTextureLayers() {
            background(shadow)
            layer("checksLarge", highlight)
            layer("checksSmall",color)
        }
    },
    DEEPSLATE_TOP(DEEPSLATE) {
        override fun LayerListBuilder.createTextureLayers() {
            copy(DEEPSLATE)
            layer("cross", shadow)
            layer("borderSolid", highlight)
        }
    },
    SANDSTONE_BASE(SimpleSoftEarth.SAND, hasOutput = false) {
        override fun LayerListBuilder.createTextureLayers() {
            background(color)
            layer("checksLarge", shadow)
        }
    },
    SANDSTONE_BOTTOM(SimpleSoftEarth.SAND) {
        override fun LayerListBuilder.createTextureLayers() {
            copy(SANDSTONE_BASE)
            layer("borderLongDashes", highlight)
        }
    },
    SANDSTONE_TOP(SimpleSoftEarth.SAND) {
        override fun LayerListBuilder.createTextureLayers() {
            copy(SANDSTONE_BASE)
            layer("borderSolidThick", shadow)
            layer("borderSolid", highlight)
        }
    },
    SANDSTONE(SimpleSoftEarth.SAND) {
        override fun LayerListBuilder.createTextureLayers() {
            background(color)
            layer("topPart", shadow)
            layer("borderSolid", shadow)
            layer("topStripeThick", highlight)
            layer("borderShortDashes", highlight)
        }
    },
    CUT_SANDSTONE(SimpleSoftEarth.SAND) {
        override fun LayerListBuilder.createTextureLayers() {
            background(color)
            layer("checksLarge", highlight)
            layer("borderSolid", shadow)
            layer("borderSolidTopLeft", highlight)
        }
    },
    CHISELED_SANDSTONE(SimpleSoftEarth.SAND) {
        override fun LayerListBuilder.createTextureLayers() {
            copy(CUT_SANDSTONE)
            layer("creeperFaceSmall", shadow)
        }
    },
    RED_SAND_BASE(SimpleSoftEarth.RED_SAND, hasOutput = false) {
        override fun LayerListBuilder.createTextureLayers() {
            background(color)
            layer("checksLarge", highlight)
        }
    },
    RED_SANDSTONE_BOTTOM(SimpleSoftEarth.RED_SAND) {
        override fun LayerListBuilder.createTextureLayers() {
            copy(RED_SAND_BASE)
            layer("borderLongDashes", shadow)
        }
    },
    RED_SANDSTONE_TOP(SimpleSoftEarth.RED_SAND) {
        override fun LayerListBuilder.createTextureLayers() {
            copy(RED_SAND_BASE)
            layer("borderSolidThick", highlight)
            layer("borderSolid", shadow)
        }
    },
    RED_SANDSTONE(SimpleSoftEarth.RED_SAND) {
        override fun LayerListBuilder.createTextureLayers() {
            background(color)
            layer("topPart",shadow)
            layer("borderSolid", highlight)
            layer("topStripeThick", highlight)
            layer("borderShortDashes", shadow)
        }
    },
    CUT_RED_SANDSTONE(SimpleSoftEarth.RED_SAND) {
        override fun LayerListBuilder.createTextureLayers() {
            copy(RED_SAND_BASE)
            layer("borderSolid", shadow)
            layer("borderSolidTopLeft", highlight)
        }
    },
    CHISELED_RED_SANDSTONE(SimpleSoftEarth.RED_SAND) {
        override fun LayerListBuilder.createTextureLayers() {
            copy(CUT_RED_SANDSTONE)
            layer("witherSymbol", shadow)
        }
    },
    BLACKSTONE_TOP(Polishable.BLACKSTONE) {
        override fun LayerListBuilder.createTextureLayers() {
            background(shadow)
            layer("bigRingsBottomLeftTopRight", color)
            layer("bigRingsTopLeftBottomRight", highlight)
        }
    },
    GILDED_BLACKSTONE(Ore.GOLD) {
        override fun LayerListBuilder.createTextureLayers() {
            copy{Polishable.BLACKSTONE.createPolishedTexture()}
            layer("bigRingsBottomLeftTopRight", color)
        }
    },
    BASALT_TOP(c(0x4e4e4e), c(0x002632), c(0x747474)) {
        override fun LayerListBuilder.createTextureLayers() {
            background(color)
            layer("bigRingsBottomLeftTopRight", highlight)
            layer("bigRingsTopLeftBottomRight", shadow)
            layer("x", color)
            layer("bigDiamond", color)
            layer("borderSolid", shadow)
            layer("borderLongDashes", highlight)
        }
    },
    BASALT_SIDE(BASALT_TOP) {
        override fun LayerListBuilder.createTextureLayers() {
            background(shadow)
            layer("stripesVerticalThick", color)
            layer("borderLongDashes", highlight)
        }
    },
    POLISHED_BASALT_TOP(BASALT_TOP) {
        override fun LayerListBuilder.createTextureLayers() {
            background(color)
            layer("ringsCentralBullseye", shadow)
            layer("rings", highlight)
            layer("cutInQuarters1", shadow)
            layer("cutInQuarters2", highlight)
            layer("borderSolid", shadow)
            layer("borderSolidTopLeft", highlight)
        }
    },
    POLISHED_BASALT_SIDE(BASALT_SIDE) {
        override fun LayerListBuilder.createTextureLayers() {
            background(color)
            layer("stripesVerticalThick", highlight)
            layer("borderSolid", shadow)
            layer("borderSolidTopLeft", highlight)
        }
    },
    GLOWSTONE(c(0xcc8654), c(0x6f4522), c(0xffda74)) {
        override fun LayerListBuilder.createTextureLayers() {
            background(color)
            layer("borderSolid", shadow)
            layer("checksSmall", highlight)
            layer("lampOn", Color.WHITE)
        }
    },
    END_STONE(c(0xdeffa4),c(0xc5be8b),c(0xffffb4)) {
        override fun LayerListBuilder.createTextureLayers() {
            background(color)
            layer("checksLargeOutline", shadow)
            layer("bigDotsTopLeftBottomRight", highlight)
        }
    },
    QUARTZ_PILLAR(Ore.QUARTZ) {
        override fun LayerListBuilder.createTextureLayers() {
            background(shadow)
            layer("tntSticksSide", color)
            layer("borderSolid", shadow)
            layer("borderDotted", highlight)
        }
    },
    QUARTZ_PILLAR_TOP(QUARTZ_PILLAR) {
        override fun LayerListBuilder.createTextureLayers() {
            background(color)
            layer("rings", highlight)
            layer("borderSolid", shadow)
            layer("borderDotted", highlight)
        }
    },
    MUD_BRICKS(SimpleSoftEarth.PACKED_MUD) {
        override fun LayerListBuilder.createTextureLayers() {
            background(color)
            layer("strokeTopLeftBottomRight2", highlight)
            layer("strokeBottomLeftTopRight2", shadow)
            layer("bricks", SimpleSoftEarth.MUD.shadow)
            layer("borderDotted", SimpleSoftEarth.MUD.highlight)
        }
    },
    STONE_BRICKS(STONE) {
        override fun LayerListBuilder.createTextureLayers() {
            background(color)
            layer("checksLarge", highlight)
            layer("bricks", stoneExtremeShadow)
            layer("borderShortDashes", shadow)
        }
    },
    CRACKED_STONE_BRICKS(STONE_BRICKS) {
        override fun LayerListBuilder.createTextureLayers() {
            copy(STONE_BRICKS)
            layer("streaks", stoneExtremeShadow)
        }
    },
    MOSSY_STONE_BRICKS(SimpleSoftEarth.MOSS_BLOCK) {
        override fun LayerListBuilder.createTextureLayers() {
            copy(STONE_BRICKS)
            layer("dots3", shadow)
            layer("dots2", highlight)
            layer("dots1", color)
            layer("borderSolid", highlight)
            layer("borderShortDashes", shadow)
        }
    },
    CHISELED_STONE_BRICKS(STONE_BRICKS) {
        override fun LayerListBuilder.createTextureLayers() {
            background(shadow)
            layer("checksLarge", highlight)
            layer("ringsCentralBullseye", stoneExtremeHighlight)
            layer("rings2", stoneExtremeShadow)
            layer("borderSolid", stoneExtremeShadow)
            layer("borderSolidTopLeft", stoneExtremeHighlight)
        }
    },
    TERRACOTTA(c(0x965d43), c(0x945b43), c(0x9b6045)) {
        override fun LayerListBuilder.createTextureLayers() {
            background(shadow)
            layer("bigRingsTopLeftBottomRight", highlight)
            layer("borderRoundDots", highlight)
        }
    },
    BRICKS(mortarColor, TERRACOTTA.shadow,  mortarColor) {
        override fun LayerListBuilder.createTextureLayers() {
            background(shadow)
            layer("bricksSmall", highlight)
            layer("borderDotted", highlight, 0.5)
        }
    },
    QUARTZ_BRICKS(Ore.QUARTZ) {
        override fun LayerListBuilder.createTextureLayers() {
            background(color)
            layer("streaks", highlight)
            layer("bricks", shadow)
            layer("borderDotted", highlight)
        }
    },
    POLISHED_BLACKSTONE_BRICKS(Polishable.BLACKSTONE) {
        override fun LayerListBuilder.createTextureLayers() {
            background(color)
            layer("bricksSmall", shadow)
            layer("borderDotted", highlight)
        }
    },
    DEEPSLATE_BRICKS(DEEPSLATE) {
        override fun LayerListBuilder.createTextureLayers() {
            copy(DEEPSLATE)
            layer("bricksSmall", shadow)
            layer("borderDotted", highlight)
            layer("borderDottedBottomRight", shadow)
        }
    },
    END_STONE_BRICKS(END_STONE) {
        override fun LayerListBuilder.createTextureLayers() {
            copy(END_STONE)
            layer("bricksSmall", shadow)
            layer("borderShortDashes", highlight)
        }
    },
    NETHER_BRICKS(c(0x302020), Color.BLACK, c(0x442727)) {
        override fun LayerListBuilder.createTextureLayers() {
            background(color)
            layer("bricksSmall", highlight)
            layer("borderDotted", highlight)
            layer("borderDottedBottomRight", shadow)
        }
    },
    RED_NETHER_BRICKS(c(0x440000),c(0x2e0000),c(0x730000)) {
        override fun LayerListBuilder.createTextureLayers() {
            background(color)
            layer("bricksSmall", shadow)
            layer("borderDotted", highlight)
            layer("borderDottedBottomRight", shadow)
        }
    },
    AMETHYST_BLOCK(c(0x7a5bb5),c(0x64479e),c(0xc890ff)) {
        override fun LayerListBuilder.createTextureLayers() {
            background(color)
            layer("triangles1", highlight)
            layer("triangles2", shadow)
        }
    },
    BUDDING_AMETHYST(AMETHYST_BLOCK.color,c(0x462b7d),AMETHYST_BLOCK.highlight) {
        override fun LayerListBuilder.createTextureLayers() {
            copy(AMETHYST_BLOCK)
            layer("buddingAmethystCenter", shadow)
        }
    },
    AMETHYST_CLUSTER(AMETHYST_BLOCK.color,BUDDING_AMETHYST.shadow,c(0xffcbff)) {
        override fun LayerListBuilder.createTextureLayers() {
            layer("amethystCluster1", highlight)
            layer("amethystCluster2", shadow)
        }
    },
    LARGE_AMETHYST_BUD(AMETHYST_BLOCK) {
        override fun LayerListBuilder.createTextureLayers() {
            layer("largeAmethystBud1", highlight)
            layer("largeAmethystBud2", shadow)
            layer("largeAmethystBud3", color)
        }
    },
    MEDIUM_AMETHYST_BUD(AMETHYST_BLOCK) {
        override fun LayerListBuilder.createTextureLayers() {
            layer("mediumAmethystBud1", highlight)
            layer("mediumAmethystBud2", shadow)
            layer("largeAmethystBud3", color)
        }
    },
    SMALL_AMETHYST_BUD(AMETHYST_BLOCK) {
        override fun LayerListBuilder.createTextureLayers() {
            layer("smallAmethystBud1", highlight)
            layer("smallAmethystBud2", shadow)
        }
    }
    ;

    override fun outputTasks(ctx: ImageProcessingContext): Flow<OutputTask> =
        if (hasOutput) super<SingleTextureMaterial>.outputTasks(ctx) else flowOf()

    constructor(base: ShadowHighlightMaterial, hasOutput: Boolean = false):
            this(base.color, base.shadow, base.highlight, hasOutput)
}