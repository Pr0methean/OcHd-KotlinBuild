package io.github.pr0methean.ochd.materials.block.pickaxe

import io.github.pr0methean.ochd.ImageProcessingContext
import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.materials.block.shovel.SimpleSoftEarth
import io.github.pr0methean.ochd.tasks.OutputTask
import io.github.pr0methean.ochd.texturebase.Block
import io.github.pr0methean.ochd.texturebase.ShadowHighlightMaterial
import io.github.pr0methean.ochd.texturebase.copy
import io.github.pr0methean.ochd.texturebase.group
import javafx.scene.paint.Paint

val SIMPLE_PICKAXE_BLOCKS = group<SimplePickaxeBlock>()
enum class SimplePickaxeBlock(
    override val color: Paint,
    override val shadow: Paint,
    override val highlight: Paint,
    val hasOutput: Boolean = true
): ShadowHighlightMaterial, Block {
    SMOOTH_STONE(OreBase.STONE) {
        override fun LayerListBuilder.createTextureLayers() {
            background(color)
            layer("borderSolid",OreBase.stoneExtremeShadow)
        }
    },
    COBBLESTONE(OreBase.STONE) {
        override fun LayerListBuilder.createTextureLayers() {
            background(highlight)
            layer("checksLarge",shadow)
            layer("checksSmall",color)
            layer("borderSolid",OreBase.stoneExtremeHighlight)
            layer("borderShortDashes", OreBase.stoneExtremeShadow)
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
    COBBLED_DEEPSLATE(OreBase.DEEPSLATE) {
        override fun LayerListBuilder.createTextureLayers() {
            background(shadow)
            layer("checksLarge", highlight)
            layer("checksSmall",color)
        }
    },
    DEEPSLATE_TOP(OreBase.DEEPSLATE) {
        override fun LayerListBuilder.createTextureLayers() {
            copy(OreBase.DEEPSLATE)
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
    };

    override fun outputTasks(ctx: ImageProcessingContext): Iterable<OutputTask> =
        if (hasOutput) super.outputTasks(ctx) else listOf()

    constructor(base: ShadowHighlightMaterial, hasOutput: Boolean = false):
            this(base.color, base.shadow, base.highlight, hasOutput)
    /*
push bigRingsBottomLeftTopRight ${andesite_h} a1 ${andesite}
push bigRingsTopLeftBottomRight ${andesite_s} a2
out_stack block/andesite

push_copy block/andesite ap1
push borderSolidBottomRight ${andesite_s} ap2
push borderSolidTopLeft ${andesite_h} ap3
out_stack block/polished_andesite

push bigRingsBottomLeftTopRight ${diorite_s} d1 ${diorite}
push bigRingsTopLeftBottomRight ${diorite_h} d2
out_stack block/diorite

push_copy block/diorite dp1
push borderSolidBottomRight ${diorite_s} dp2
push borderSolidTopLeft ${diorite_h} dp3
out_stack block/polished_diorite

push bigDotsBottomLeftTopRight ${granite_s} g11 ${granite}
push bigDotsTopLeftBottomRight ${granite_h} g12
push bigRingsBottomLeftTopRight ${granite_h} g21
push bigRingsTopLeftBottomRight ${granite_s} g22
out_stack block/granite

push_copy block/granite gp1
push borderSolidBottomRight ${granite_s} gp2
push borderSolidTopLeft ${granite_h} gp3
out_stack block/polished_granite
     */
}