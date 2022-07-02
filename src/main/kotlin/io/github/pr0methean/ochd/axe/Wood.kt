package io.github.pr0methean.ochd.axe

import io.github.pr0methean.ochd.ImageProcessingContext
import io.github.pr0methean.ochd.LayerList
import io.github.pr0methean.ochd.c
import io.github.pr0methean.ochd.color.Material
import io.github.pr0methean.ochd.color.ShadowHighlightMaterial
import io.github.pr0methean.ochd.pickaxe.OreBase.STONE
import io.github.pr0methean.ochd.tasks.OutputTask
import javafx.scene.paint.Color

sealed interface Wood: ShadowHighlightMaterial {
    val barkColor: Color
    val barkHighlight: Color
    val barkShadow: Color
    val logSynonym: String
    fun LayerList.bark(): Unit
    fun LayerList.strippedLogSide(): Unit
    fun LayerList.logTop(): Unit
    fun LayerList.strippedLogTop(): Unit
    fun LayerList.planks(): Unit
    fun LayerList.trapdoor(): Unit

    override fun outputTasks(ctx: ImageProcessingContext): Iterable<OutputTask> = listOf(
        ctx.out("block/${name}_${logSynonym}", ctx.stack { bark() }),
        ctx.out("block/${name}_${logSynonym}_top", ctx.stack { logTop() }),
        ctx.out("block/stripped_${name}_${logSynonym}", ctx.stack { strippedLogSide() }),
        ctx.out("block/stripped_${name}_${logSynonym}_top", ctx.stack {strippedLogTop()}),
        ctx.out("block/${name}_planks", ctx.stack { planks() }),
        ctx.out("block/${name}_trapdoor", ctx.stack { trapdoor() })
    )
}

enum class OverworldWood(
    override val color: Color,
    override val highlight: Color,
    override val shadow: Color,
    override val barkColor: Color,
    override val barkHighlight: Color,
    override val barkShadow: Color)
    : Wood {
    ACACIA(
        color = Color.rgb(0xad, 0x5d, 0x32),
        highlight = Color.rgb(0xc2, 0x6d, 0x3f),
        shadow = Color.rgb(0x8f, 0x4c, 0x2a),
        barkColor = c(0x696259),
        barkHighlight = c(0x8d8477),
        barkShadow = c(0x504b40),
    ) {
        override fun LayerList.trapdoor() {
            layer("bigDiamond", color)
            layer("borderSolidThick", color)
            layer("borderSolid", highlight)
            layer("trapdoorHingesBig", STONE.shadow)
            layer("trapdoorHinges", STONE.highlight)
        }
    },
    BIRCH(
        color = c(0xc8b77a),
        highlight = c(0xd7cb8d),
        shadow = c(0x9e8b61),
        barkColor = c(0xeeffea),
        barkHighlight = Color.WHITE,
        barkShadow = c(0x605e54)
    ) {
        override fun LayerList.trapdoor() {
            layer("trapdoor1", color)
            layer("borderSolid", shadow)
            layer("trapdoorHingesBig", STONE.shadow)
        }
    },
    DARK_OAK(
        color = c(0x3e2912),
        shadow = c(0x3a2411),
        highlight = c(0x53381a),
        barkColor = c(0x3f3100),
        barkShadow = c(0x292000),
        barkHighlight = c(0x584428)
    ) {
        override fun LayerList.trapdoor() {
            background(color)
            layer("2x2BottomRight", highlight)
            layer("2x2TopLeft", shadow)
            layer("borderShortDashes", color)
            layer("trapdoorHingesBig", STONE.highlight)
        }
    },
    JUNGLE(
        color = c(0x9f714a),
        shadow = c(0x785437),
        highlight = c(0x7d5d26),
        barkColor = c(0x503f00),
        barkShadow = c(0x3e3000),
        barkHighlight = c(0x7d5d26)
    ) {
        override fun LayerList.trapdoor() {
            layer("trapdoor2", color)
            layer("borderSolid", shadow)
            layer("borderShortDashes", highlight)
            layer("trapdoorHingesBig", STONE.shadow)
            layer("trapdoorHinges", STONE.color)
        }
    },
    MANGROVE(
        color = c(0x773934),
        highlight = c(0x8b4d3a),
        shadow = c(0x5d0000),
        barkColor = c(0x5a482c),
        barkHighlight = c(0x675230),
        barkShadow = c(0x443522)
    ) {
        override fun LayerList.trapdoor() {
            layer("ringsHole", color)
            layer("rings2", shadow)
            layer("borderDotted", highlight)
            layer("trapdoorHingesBig", STONE.highlight)
            layer("trapdoorHinges", STONE.shadow)
        }
    };


    override fun LayerList.bark() {
        background(barkColor)
        layer("borderSolid", barkShadow)
        layer("borderDotted", barkHighlight)
        layer("zigzagSolid", barkShadow)
        layer("zigzagSolid2", barkHighlight)
    }

    override fun LayerList.strippedLogSide() {
        background(color)
        layer("borderSolid", shadow)
        layer("borderShortDashes", highlight)
    }

    override fun LayerList.logTop() {
        copy {strippedLogTop()}
        layer("borderSolid", barkColor)
        layer("borderDotted", barkShadow)
    }

    override fun LayerList.strippedLogTop() {
        copy { strippedLogSide() }
        layer("ringsCentralBullseye", highlight)
        layer("rings", shadow)
    }

    override fun LayerList.planks() {
        background(color)
        layer("waves", highlight)
        layer("planksTopBorder", shadow)
        layer("borderShortDashes", highlight)
    }

    override val logSynonym = "log"
}
enum class Fungus(
        override val color: Color,
        override val highlight: Color,
        override val shadow: Color,
        override val barkColor: Color,
        override val barkHighlight: Color,
        override val barkShadow: Color)
    : Wood {
        CRIMSON, WARPED;

    override val logSynonym = "stem"
}
/*
wood_crimson_h='863e5a'
wood_crimson='6a344b'
wood_crimson_s='4b2737'


wood_oak_h='c29d62'
wood_oak='af8f55'
wood_oak_s='7e6237'
wood_spruce_h='886539'
wood_spruce='70522e'
wood_spruce_s='5a4424'
wood_warped_h='3a8e8c'
wood_warped='287067'
wood_warped_s='1e4340'
bark_crimson_h='b10000'
bark_crimson='4b2737'
bark_crimson_s='442131'
bark_oak_h='987849'
bark_oak='745a36'
bark_oak_s='4c3d26'
bark_spruce_h='553a1f'
bark_spruce='3b2700'
bark_spruce_s='2e1c00'
bark_warped_h='00956f'
bark_warped='562c3e'
bark_warped_s='442131'
*/