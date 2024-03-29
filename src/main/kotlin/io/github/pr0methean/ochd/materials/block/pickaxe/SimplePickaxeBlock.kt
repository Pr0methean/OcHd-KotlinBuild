package io.github.pr0methean.ochd.materials.block.pickaxe

import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.c
import io.github.pr0methean.ochd.materials.block.pickaxe.OreBase.Companion.stoneExtremeHighlight
import io.github.pr0methean.ochd.materials.block.pickaxe.OreBase.Companion.stoneExtremeShadow
import io.github.pr0methean.ochd.materials.block.pickaxe.OreBase.DEEPSLATE
import io.github.pr0methean.ochd.materials.block.pickaxe.OreBase.STONE
import io.github.pr0methean.ochd.materials.block.shovel.SimpleSoftEarth
import io.github.pr0methean.ochd.texturebase.Block
import io.github.pr0methean.ochd.texturebase.ShadowHighlightMaterial
import io.github.pr0methean.ochd.texturebase.SingleTextureMaterial
import javafx.scene.paint.Color
import javafx.scene.paint.Color.WHITE
import javafx.scene.paint.Paint

val mortarColor: Color = c(0xa2867d)
@Suppress("unused")
enum class SimplePickaxeBlock(
    override val color: Paint,
    override val shadow: Paint,
    override val highlight: Paint,
    override val hasOutput: Boolean = true
): SingleTextureMaterial, ShadowHighlightMaterial, Block {
    SMOOTH_STONE(STONE) {
        override fun LayerListBuilder.createTextureLayers() {
            background(color)
            layer("borderSolid", stoneExtremeShadow)
        }
    },
    COBBLESTONE_BASE(STONE, false) {
        override fun LayerListBuilder.createTextureLayers() {
            background(highlight)
            layer("checksLarge",shadow)
            layer("checksSmall",color)
        }
    },
    COBBLESTONE(STONE) {
        override fun LayerListBuilder.createTextureLayers() {
            copy(COBBLESTONE_BASE)
            layer("borderSolid", stoneExtremeHighlight)
            layer("borderShortDashes", stoneExtremeShadow)
        }
    },
    MOSSY_COBBLESTONE(SimpleSoftEarth.MOSS_BLOCK) {
        override fun LayerListBuilder.createTextureLayers() {
            copy(COBBLESTONE_BASE)
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
            background(color)
            layer("borderSolid", highlight)
            layer("checksLarge", shadow)
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
            layer("checksLargeOutline", highlight)
            layer("borderSolid", shadow)
            layer("borderSolidTopLeft", highlight)
            layer("borderLongDashes", color)
        }
    },
    CHISELED_SANDSTONE(SimpleSoftEarth.SAND) {
        override fun LayerListBuilder.createTextureLayers() {
            copy(CUT_SANDSTONE)
            layer("creeperFaceSmall", shadow)
        }
    },
    RED_SANDSTONE_BASE(SimpleSoftEarth.RED_SAND, hasOutput = false) {
        override fun LayerListBuilder.createTextureLayers() {
            background(color)
            layer("checksLarge", highlight)
            layer("checksLargeOutline", shadow)
        }
    },
    RED_SANDSTONE_BOTTOM(SimpleSoftEarth.RED_SAND) {
        override fun LayerListBuilder.createTextureLayers() {
            copy(RED_SANDSTONE_BASE)
            layer("borderLongDashes", color)
        }
    },
    RED_SANDSTONE_TOP(SimpleSoftEarth.RED_SAND) {
        override fun LayerListBuilder.createTextureLayers() {
            copy(RED_SANDSTONE_BASE)
            layer("borderSolidThick", shadow)
            layer("borderSolid", highlight)
            layer("borderLongDashes", color)
        }
    },
    CUT_RED_SANDSTONE(SimpleSoftEarth.RED_SAND) {
        override fun LayerListBuilder.createTextureLayers() {
            background(color)
            layer("checksLarge", highlight)
            layer("borderSolid", shadow)
            layer("borderSolidTopLeft", highlight)
            layer("borderLongDashes", color)
        }
    },
    CHISELED_RED_SANDSTONE(SimpleSoftEarth.RED_SAND) {
        override fun LayerListBuilder.createTextureLayers() {
            copy(CUT_RED_SANDSTONE)
            layer("witherSymbol", shadow)
        }
    },
    RED_SANDSTONE(SimpleSoftEarth.RED_SAND) {
        override fun LayerListBuilder.createTextureLayers() {
            background(color)
            layer("topPart",shadow)
            layer("topStripeThick", highlight)
            layer("borderSolid", highlight)
            layer("borderShortDashes", shadow)
        }
    },
    BASALT_TOP(c(0x515151), c(0x003939), c(0x737373)) {
        override fun LayerListBuilder.createTextureLayers() {
            background(color)
            layer("borderSolid", highlight)
            layer("borderLongDashes", shadow)
            layer("bigRingsBottomLeftTopRight", highlight)
            layer("bigRingsTopLeftBottomRight", shadow)
            layer("strokeBottomLeftTopRight", color)
            layer("strokeTopLeftBottomRight", color)
            layer("bigDiamond", color)
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
            layer("borderSolid", shadow)
            layer("borderSolidTopLeft", highlight)
            layer("cross", highlight)
            layer("crossDotted", shadow)
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
            layer("lampOn", WHITE)
        }
    },
    END_STONE(c(0xdeffa4),c(0xc5be8b),c(0xffffb4)) {
        override fun LayerListBuilder.createTextureLayers() {
            background(color)
            layer("checksLargeOutline", shadow)
            layer("checksQuarterCircles", shadow)
            layer("bigRingsTopLeftBottomRight", highlight)
        }
    },
    END_STONE_BRICKS(END_STONE) {
        override fun LayerListBuilder.createTextureLayers() {
            background(highlight)
            layer("checksSmall", color)
            layer("bricksSmall", shadow)
            layer("borderShortDashes", highlight)
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
            layer("bricks", SimpleSoftEarth.MUD.shadow)
            layer("strokeTopLeftBottomRight2", highlight)
            layer("strokeBottomLeftTopRight2", shadow)
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
        override fun LayerListBuilder.createTextureLayers() {
            background(color)
            layer("ringsCentralBullseye", stoneExtremeHighlight)
            layer("rings2", stoneExtremeShadow)
            layer("borderSolid", stoneExtremeShadow)
            layer("borderSolidTopLeft", stoneExtremeHighlight)
        }
    },
    TERRACOTTA(c(0x945b43), c(0x945b43), c(0x9b6045)) {
        override fun LayerListBuilder.createTextureLayers() {
            background(color)
            copy {
                layer("bigDotsTopLeftBottomRight", shadow)
                layer("bigDotsBottomLeftTopRight", highlight)
            }
            copy {
                layer("bigRingsTopLeftBottomRight", highlight)
                layer("bigRingsBottomLeftTopRight", shadow)
            }
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
    NETHER_BRICKS(c(0x302020), Color.BLACK, c(0x442929)) {
        override fun LayerListBuilder.createTextureLayers() {
            background(color)
            layer("bricksSmall", highlight)
            layer("borderDotted", highlight)
            layer("borderDottedBottomRight", shadow)
        }
    },
    RED_NETHER_BRICKS(c(0x500000),c(0x2e0000),c(0x730000)) {
        override fun LayerListBuilder.createTextureLayers() {
            background(color)
            layer("bricksSmall", shadow)
            layer("borderDotted", highlight)
            layer("borderDottedBottomRight", shadow)
        }
    },
    AMETHYST_BLOCK(c(0xc890ff),c(0x7a5bb5),c(0xffcbff)) {
        override fun LayerListBuilder.createTextureLayers() {
            background(shadow)
            layer("triangles1", highlight)
            layer("triangles2", color)
        }
    },
    BUDDING_AMETHYST(AMETHYST_BLOCK.color,c(0x462b7d),AMETHYST_BLOCK.highlight) {
        override fun LayerListBuilder.createTextureLayers() {
            copy(AMETHYST_BLOCK)
            layer("buddingAmethystCenter", shadow)
        }
    },
    AMETHYST_CLUSTER(AMETHYST_BLOCK) {
        override fun LayerListBuilder.createTextureLayers() {
            layer("amethystCluster1", highlight)
            layer("amethystCluster2", color)
        }
    },
    LARGE_AMETHYST_BUD(AMETHYST_BLOCK) {
        override fun LayerListBuilder.createTextureLayers() {
            layer("largeAmethystBud1", color)
            layer("largeAmethystBud2", shadow)
            layer("largeAmethystBud3", highlight)
        }
    },
    MEDIUM_AMETHYST_BUD(AMETHYST_BLOCK) {
        override fun LayerListBuilder.createTextureLayers() {
            layer("mediumAmethystBud1", color)
            layer("mediumAmethystBud2", shadow)
            layer("largeAmethystBud3", highlight)
        }
    },
    SMALL_AMETHYST_BUD(AMETHYST_BLOCK) {
        override fun LayerListBuilder.createTextureLayers() {
            layer("smallAmethystBud1", color)
            layer("smallAmethystBud2", shadow)
        }
    },
    BLACK_GLAZED_TERRACOTTA(c(0x2f2f2f), Color.BLACK, c(0x992222)) {
        override fun LayerListBuilder.createTextureLayers() {
            background(shadow)
            layer("asymmetricalQuarterCircles", color)
            layer("bigRingsBottomLeftTopRight", highlight)
            layer("cornerRoundTopLeft", highlight)
        }
    },
    BLUE_GLAZED_TERRACOTTA(c(0x4040aa), c(0x2d2d8f), c(0x4577d3)) {
        override fun LayerListBuilder.createTextureLayers() {
            background(shadow)
            layer("checksQuarterCircles", color)
            layer("bigDotsTopLeftBottomRight")
            layer("bigRingsTopLeftBottomRight", color)
            layer("checksLargeOutline", highlight)
            layer("cornerRingTopLeft", highlight)
        }
    },
    BROWN_GLAZED_TERRACOTTA(c(0x8c5a35), c(0x007788), c(0xcd917c)) {
        override fun LayerListBuilder.createTextureLayers() {
            background(color)
            layer("cornersRound", shadow)
            layer("ray", highlight)
            layer("cornerCrosshairs", highlight)
        }
    },
    GRAY_GLAZED_TERRACOTTA(c(0x737373), c(0x333333), c(0x999999)) {
        override fun LayerListBuilder.createTextureLayers() {
            background(color)
            layer("asymmetricalQuarterCircles", shadow)
            layer("cornerCrosshairs", highlight)
            layer("cornerRoundTopLeft", highlight)
            layer("comparator", highlight)
            layer("repeaterSideInputs", highlight)
        }
    },
    GREEN_GLAZED_TERRACOTTA(c(0x729b24), c(0x495b24), c(0xd6d6d6)) {
        override fun LayerListBuilder.createTextureLayers() {
            background(color)
            layer("railCorner", shadow)
            layer("strokeTopLeftBottomRight", highlight)
            layer("cornerRingTopLeft", highlight)
        }
    },
    RED_GLAZED_TERRACOTTA(c(0xb82b2b), c(0x8e2020), c(0xce4848)) {
        override fun LayerListBuilder.createTextureLayers() {
            background(color)
            layer("cornersRound", highlight)
            layer("topPart", color)
            layer("ringsSpiral", highlight)
            layer("cornerRoundTopLeft", shadow)
        }
    },
    PINK_GLAZED_TERRACOTTA(c(0xff8baa), c(0xd6658f), c(0xffb5cb)) {
        override fun LayerListBuilder.createTextureLayers() {
            background(shadow)
            layer("strokeTopLeftBottomRight4", highlight)
            layer("cornersTri", shadow)
            layer("fishTail", shadow)
            layer("fishFins", shadow)
            layer("fishBody", color)
        }
    },
    MAGENTA_GLAZED_TERRACOTTA(c(0xdc68dc), c(0xae33ae), c(0xffa5bf)) {
        override fun LayerListBuilder.createTextureLayers() {
            background(shadow)
            layer("stripesVerticalThick", color)
            layer("arrowUpExpanded", highlight)
            layer("arrowUp", shadow)
        }
    },
    CYAN_GLAZED_TERRACOTTA(c(0x828282), c(0x3a3a3a), c(0x009c9c)) {
        override fun LayerListBuilder.createTextureLayers() {
            background(color)
            layer("strokeBottomLeftTopRight4", highlight)
            layer("strokeBottomLeftTopRight2", shadow)
            layer("craftingGridSquare", highlight)
            layer("creeperFaceSmall", shadow)
        }
    },
    LIGHT_BLUE_GLAZED_TERRACOTTA(c(0x2389c7), c(0x2d2d8f), c(0x57bddf)) {
        override fun LayerListBuilder.createTextureLayers() {
            // TODO: maybe add the parallelogram-shaped pieces and white corners?
            background(shadow)
            layer("bottomHalf", WHITE)
            layer("checksLarge", highlight)
            layer("emeraldTopLeft", WHITE)
            layer("emeraldBottomRight", color)
        }
    },
    LIME_GLAZED_TERRACOTTA(c(0x8bd922), c(0x5ea900), c(0xffffc4)) {
        override fun LayerListBuilder.createTextureLayers() {
            background(color)
            layer("borderSolidTopLeft", shadow)
            layer("strokeTopLeftBottomRight", shadow)
            layer("railCornerInverted", highlight)
        }
    },
    ;

    constructor(base: ShadowHighlightMaterial, hasOutput: Boolean = true):
            this(base.color, base.shadow, base.highlight, hasOutput)
}
