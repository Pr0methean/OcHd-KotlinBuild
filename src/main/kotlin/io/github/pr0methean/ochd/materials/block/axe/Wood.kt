package io.github.pr0methean.ochd.materials.block.axe

import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.TaskPlanningContext
import io.github.pr0methean.ochd.c
import io.github.pr0methean.ochd.materials.block.pickaxe.OreBase.STONE
import io.github.pr0methean.ochd.materials.block.shovel.DirtGroundCover
import io.github.pr0methean.ochd.tasks.AbstractImageTask
import io.github.pr0methean.ochd.tasks.InvalidTask
import io.github.pr0methean.ochd.tasks.PngOutputTask
import io.github.pr0methean.ochd.texturebase.ShadowHighlightMaterial
import javafx.scene.paint.Color
import javafx.scene.paint.Paint

sealed interface Wood: ShadowHighlightMaterial {
    val barkColor: Paint
    val barkHighlight: Paint
    val barkShadow: Paint
    val leavesColor: Paint
    val leavesHighlight: Paint
    val leavesShadow: Paint
    val logSynonym: String
    val leavesSynonym: String
    val saplingSynonym: String
    val name: String
    fun LayerListBuilder.bark()
    fun LayerListBuilder.strippedLogSide()
    fun LayerListBuilder.logTop(strippedLogTop: AbstractImageTask)
    fun LayerListBuilder.strippedLogTop(strippedLogSide: AbstractImageTask)
    fun LayerListBuilder.trapdoor(commonLayers: AbstractImageTask)
    fun LayerListBuilder.doorTop(doorBottom: AbstractImageTask, commonLayers: AbstractImageTask) {
        copy(doorBottom)
        layer("doorKnob")
    }
    fun LayerListBuilder.doorBottom(commonLayers: AbstractImageTask)

    fun LayerListBuilder.leaves()
    fun LayerListBuilder.sapling()
    fun LayerListBuilder.doorCommonLayers()

    override fun outputTasks(ctx: TaskPlanningContext): Sequence<PngOutputTask> = sequence {
        val doorCommonLayers = ctx.stack { doorCommonLayers() }
        val doorBottom = ctx.stack { doorBottom(doorCommonLayers) }
        val strippedLogSide = ctx.stack { strippedLogSide() }
        val strippedLogTop = ctx.stack { strippedLogTop(strippedLogSide) }
        yield(ctx.out(ctx.stack { bark() }, "block/${name}_${logSynonym}"))
        yield(ctx.out(strippedLogSide, "block/stripped_${name}_${logSynonym}"))
        yield(ctx.out(strippedLogTop, "block/stripped_${name}_${logSynonym}_top"))
        yield(ctx.out(ctx.stack { logTop(strippedLogTop) }, "block/${name}_${logSynonym}_top"))
        yield(ctx.out(ctx.stack { trapdoor(doorCommonLayers) }, "block/${name}_trapdoor"))
        yield(ctx.out(ctx.stack { doorTop(doorBottom, doorCommonLayers) }, "block/${name}_door_top"))
        yield(ctx.out(doorBottom, "block/${name}_door_bottom"))
        yield(ctx.out(ctx.stack { leaves() }, "block/${name}_${leavesSynonym}"))
        yield(ctx.out(ctx.stack { sapling() }, "block/${name}_${saplingSynonym}"))
        yield(ctx.out(ctx.stack { planks() }, "block/${name}_planks"))
    }

    fun LayerListBuilder.planks() {
        copy {
            background(color)
            layer("waves", shadow)
            layer("waves2", highlight)
            layer("planksTopBorder", shadow)
        }
        layer("borderShortDashes", highlight)
    }
}

@Suppress("unused")
enum class OverworldWood(
    override val color: Paint,
    override val highlight: Paint,
    override val shadow: Paint,
    override val barkColor: Paint,
    override val barkHighlight: Paint,
    override val barkShadow: Paint,
    )
    : Wood {
    ACACIA(
        color = c(0xad5d32),
        highlight = c(0xc26d3f),
        shadow = c(0x915431),
        barkColor = c(0x70583B),
        barkHighlight = c(0x898977),
        barkShadow = c(0x4a4a39),
    ) {
        override fun LayerListBuilder.doorCommonLayers() {
            layer("borderSolidThick", color)
            layer("borderSolid", highlight)
            layer("bigDiamond", shadow)
        }

        override fun LayerListBuilder.trapdoor(commonLayers: AbstractImageTask) {
            copy(commonLayers)
            copy {
                layer("trapdoorHingesBig", STONE.shadow)
                layer("trapdoorHinges", STONE.highlight)
            }
        }

        override fun LayerListBuilder.doorBottom(commonLayers: AbstractImageTask) {
            copy(commonLayers)
            layer("strokeBottomLeftTopRight", color)
            layer("strokeTopLeftBottomRight", color)
            layer("doorHingesBig", STONE.shadow)
            layer("doorHinges", STONE.highlight)
        }

        override fun LayerListBuilder.leaves() {
            layer("leaves1", leavesShadow)
            layer("leaves1a", leavesHighlight)
        }

        override fun LayerListBuilder.sapling() {
            layer("saplingStem", barkColor)
            layer("acaciaSapling", c(0x6c9e38))
            layer("acaciaSapling2", c(0xc9d7a5))
        }
    },
    BIRCH(
        color = c(0xc8b77a),
        highlight = c(0xD7C187),
        shadow = c(0x915431),
        barkColor = c(0xeeffea),
        barkHighlight = Color.WHITE,
        barkShadow = c(0x5f5f4f)
    ) {
        override fun LayerListBuilder.doorCommonLayers() {
            background(Color.WHITE)
            layer("borderSolidExtraThick", color)
            layer("craftingGridSquare", highlight)
            layer("craftingGridSpaces", Color.WHITE)
            layer("borderSolid", shadow)
        }

        override fun LayerListBuilder.trapdoor(commonLayers: AbstractImageTask) {
            copy(commonLayers)
            layer("trapdoorHingesBig", STONE.shadow)
        }

        override fun LayerListBuilder.doorTop(doorBottom: AbstractImageTask, commonLayers: AbstractImageTask) {
            copy(commonLayers)
            layer("doorHingesBig", STONE.shadow)
            layer("doorKnob")
        }

        override fun LayerListBuilder.doorBottom(commonLayers: AbstractImageTask) {
            background(highlight)
            layer("borderSolidExtraThick", color)
            layer("craftingGridSquare", shadow)
            layer("craftingGridSpaces", highlight)
            layer("borderSolid", shadow)
            layer("doorHingesBig", STONE.shadow)
        }

        override fun LayerListBuilder.leaves() {
            layer("leaves2", leavesHighlight)
            layer("leaves2a", leavesShadow)
        }

        override fun LayerListBuilder.sapling() {
            layer("saplingStem", barkColor)
            layer("flowerStemBottomBorder", barkShadow)
            layer("saplingLeaves", c(0x6c9e38))
        }
    },
    DARK_OAK(
        color = c(0x3f2d23),
        shadow = c(0x3a2400),
        highlight = c(0x4a4a39),
        barkColor = c(0x483800),
        barkShadow = c(0x2b2000),
        barkHighlight = c(0x624033)
    ) {
        override fun LayerListBuilder.doorCommonLayers() {
            background(color)
            layer("borderSolid", highlight)
            layer("cross", highlight)
            layer("2x2TopLeft", shadow)
            layer("borderShortDashes", color)
        }

        override fun LayerListBuilder.trapdoor(commonLayers: AbstractImageTask) {
            copy(commonLayers)
            layer("trapdoorHingesBig", STONE.highlight)
        }

        override fun LayerListBuilder.doorBottom(commonLayers: AbstractImageTask) {
            copy(commonLayers)
            layer("doorHingesBig", STONE.highlight)
        }

        override fun LayerListBuilder.leaves() {
            layer("leaves3", leavesShadow)
            layer("leaves3a", leavesHighlight)
        }

        override fun LayerListBuilder.sapling() {
            layer("saplingStem", barkColor)
            layer("bigCircle", c(0x005c00))
            layer("bigCircleTwoQuarters", c(0x57ad3f))
        }
    },
    JUNGLE(
        color = c(0x915431),
        shadow = c(0x795b4b),
        highlight = c(0x8A593A),
        barkColor = c(0x483800),
        barkShadow = c(0x2B2000),
        barkHighlight = c(0x8A593A)
    ) {
        override fun LayerListBuilder.doorCommonLayers() {
            layer("doorHingesBig", STONE.shadow)
            layer("doorHinges", STONE.color)
        }

        override fun LayerListBuilder.trapdoor(commonLayers: AbstractImageTask) {
            layer("trapdoor2", color)
            layer("borderSolid", shadow)
            layer("borderShortDashes", highlight)
            layer("trapdoorHingesBig", STONE.shadow)
            layer("trapdoorHinges", STONE.color)
        }

        override fun LayerListBuilder.doorTop(doorBottom: AbstractImageTask, commonLayers: AbstractImageTask) {
            layer("trapdoor2", color)
            layer("borderShortDashes", highlight)
            copy(commonLayers)
            layer("doorKnob")
        }

        override fun LayerListBuilder.doorBottom(commonLayers: AbstractImageTask) {
            background(color)
            layer("waves", highlight)
            layer("planksTopBorderVertical", shadow)
            layer("borderSolid", color)
            layer("borderShortDashes", highlight)
            copy(commonLayers)
        }

        override fun LayerListBuilder.leaves() {
            layer("leaves6", leavesHighlight)
            layer("leaves6a", leavesShadow)
        }

        override fun LayerListBuilder.sapling() {
            layer("saplingStem", barkColor)
            layer("acaciaSapling", c(0x378020))
        }
    },
    MANGROVE(
        color = c(0x773636),
        highlight = c(0x8A593A),
        shadow = c(0x5d0000),
        barkColor = c(0x583838),
        barkHighlight = c(0x624033),
        barkShadow = c(0x4a4a39)
    ) {
        override val saplingSynonym: String = "propagule"

        override fun LayerListBuilder.doorCommonLayers() {
            layer("rings2", shadow)
            layer("borderDotted", highlight)
        }

        override fun LayerListBuilder.trapdoor(commonLayers: AbstractImageTask) {
            layer("ringsHole", color)
            copy(commonLayers)
            layer("trapdoorHingesBig", STONE.highlight)
            layer("trapdoorHinges", STONE.shadow)
        }

        override fun LayerListBuilder.doorBottom(commonLayers: AbstractImageTask) {
            background(color)
            copy(commonLayers)
            layer("doorHingesBig", STONE.highlight)
            layer("doorHinges", STONE.shadow)
        }

        override fun LayerListBuilder.leaves() {
            layer("leaves5", leavesHighlight)
            layer("leaves5a", leavesColor)
            layer("leaves5b", leavesShadow)
        }

        override fun LayerListBuilder.sapling() {
            layer("mangrovePropagule", c(0x4aa54a))
            layer("flowerStemBottomBorder", c(0x748241))
        }
    },
    SPRUCE(
        color = c(0x70583B),
        highlight = c(0x8A593A),
        shadow = c(0x624033),
        barkColor = c(0x3b2700),
        barkHighlight = c(0x624033),
        barkShadow = c(0x2b2000)
    ) {
        override fun LayerListBuilder.doorCommonLayers(): Unit = copy(InvalidTask)

        override fun LayerListBuilder.trapdoor(commonLayers: AbstractImageTask) {
            background(shadow)
            layer("planksTopVertical", color)
            layer("borderSolidThick", shadow)
            layer("borderLongDashes", highlight)
            layer("trapdoorHingesBig", STONE.color)
            layer("trapdoorHinges", STONE.shadow)
        }

        override fun LayerListBuilder.doorBottom(commonLayers: AbstractImageTask) {
            copy {
                planks()
            }
            layer("doorHingesBig", STONE.color)
            layer("doorHinges", STONE.shadow)
        }

        override fun LayerListBuilder.leaves() {
            layer("leaves3", leavesHighlight)
            layer("leaves3b", leavesShadow)
        }

        override fun LayerListBuilder.sapling() {
            layer("saplingStem", barkHighlight)
            layer("spruceSapling", c(0x2e492e))
        }
    },
    OAK(
        color = c(0xaf8f55),
        highlight = c(0xC29d62),
        shadow = c(0x70583B),
        barkColor = c(0x70583B),
        barkHighlight = c(0x987849),
        barkShadow = c(0x4a4a39)
    ) {
        override fun LayerListBuilder.doorCommonLayers() {
            copy {
                layer("borderSolidThick", color)
                layer("borderSolid", highlight)
            }
            layer("cross", highlight)
            copy {
                layer("2x2TopLeft", shadow)
                layer("borderShortDashes", color, 0.5)
            }
        }

        override fun LayerListBuilder.trapdoor(commonLayers: AbstractImageTask) {
            copy(commonLayers)
            layer("trapdoorHingesBig", STONE.color)
            layer("trapdoorHinges", STONE.highlight)
        }

        override fun LayerListBuilder.doorTop(doorBottom: AbstractImageTask, commonLayers: AbstractImageTask) {
            copy {
                layer("borderSolidThick", color)
                layer("borderSolid", highlight)
            }
            copy {
                layer("2x2TopLeft", shadow)
                layer("borderShortDashes", color, 0.5)
            }
            layer("craftingSide", shadow)
            layer("cross", shadow)
            layer("doorKnob")
            copy {
                layer("doorHingesBig", STONE.color)
                layer("doorHinges", STONE.highlight)
            }
        }

        override fun LayerListBuilder.doorBottom(commonLayers: AbstractImageTask) {
            background(color)
            layer("waves", highlight)
            copy(commonLayers)
            copy {
                layer("doorHingesBig", STONE.color)
                layer("doorHinges", STONE.highlight)
            }
        }

        override fun LayerListBuilder.leaves() {
            layer("leaves4", leavesShadow)
            layer("leaves4a", leavesHighlight)
        }

        override fun LayerListBuilder.sapling() {
            layer("coalBorder", c(0x005c00))
            layer("saplingStem", barkColor)
            layer("coal", c(0x57ad3f))
            layer("sunflowerPistil", c(0x005c00))
        }
    };


    override fun LayerListBuilder.bark() {
        background(barkColor)
        layer("borderSolid", barkShadow)
        layer("borderDotted", barkHighlight)
        layer("zigzagSolid", barkShadow)
        layer("zigzagSolid2", barkHighlight)
    }

    override fun LayerListBuilder.strippedLogSide() {
        background(color)
        layer("borderSolid", shadow)
        layer("borderShortDashes", highlight)
    }

    override fun LayerListBuilder.logTop(strippedLogTop: AbstractImageTask) {
        copy(strippedLogTop)
        layer("borderSolid", barkColor)
        layer("borderDotted", barkShadow)
    }

    override fun LayerListBuilder.strippedLogTop(strippedLogSide: AbstractImageTask) {
        copy(strippedLogSide)
        layer("ringsCentralBullseye", highlight)
        layer("rings", shadow)
    }

    override val logSynonym: String = "log"
    override val leavesSynonym: String = "leaves"
    override val saplingSynonym: String = "sapling"

    // Like grass, leaves are stored as gray and colorized in real time based on the biome
    override val leavesColor: Paint = DirtGroundCover.GRASS_BLOCK.color
    override val leavesHighlight: Paint = DirtGroundCover.GRASS_BLOCK.highlight
    override val leavesShadow: Paint = DirtGroundCover.GRASS_BLOCK.shadow
}

private val fungusSpotColor = c(0xff6500)
@Suppress("unused", "LongParameterList")
enum class Fungus(
        override val color: Paint,
        override val highlight: Paint,
        override val shadow: Paint,
        override val barkColor: Paint,
        override val barkHighlight: Paint,
        override val barkShadow: Paint,
        override val leavesColor: Paint,
        override val leavesHighlight: Paint,
        override val leavesShadow: Paint)
    : Wood {
        CRIMSON(
            color = c(0x6a344b),
            shadow = c(0x4b2737),
            highlight = c(0x863e5a),
            barkColor = c(0x4b2737),
            barkShadow = c(0x442929),
            barkHighlight = c(0xb10000),
            leavesColor = c(0x7b0000),
            leavesShadow = c(0x5a0000),
            leavesHighlight = c(0xac2020),
        ) {
            override fun LayerListBuilder.doorCommonLayers() = copy(InvalidTask)

            override fun LayerListBuilder.trapdoor(commonLayers: AbstractImageTask) {
                layer("borderSolidThick", color)
                layer("trapdoor1", shadow)
                layer("borderShortDashes", highlight)
                layer("zigzagSolid2", highlight)
                layer("zigzagSolid", shadow)
                layer("trapdoorHingesBig", STONE.highlight)
                layer("trapdoorHinges", STONE.shadow)
            }

            override fun LayerListBuilder.doorBottom(commonLayers: AbstractImageTask) {
                background(color)
                layer("planksTopBorderVertical", shadow)
                layer("borderShortDashes", highlight)
                layer("zigzagSolid2", barkHighlight)
                layer("zigzagSolid", shadow)
                layer("doorHingesBig", STONE.highlight)
                layer("doorHinges", STONE.shadow)
            }

            override fun LayerListBuilder.leaves() {
                background(leavesColor)
                layer("leaves6", leavesShadow)
                layer("leaves6a", leavesHighlight)
                layer("borderRoundDots", leavesHighlight)
            }
            override fun LayerListBuilder.sapling() {
                layer("mushroomStem", barkShadow)
                layer("mushroomCapRed", leavesColor)
                layer("crimsonFungusSpots", fungusSpotColor)
            }
        }, WARPED(
            color = c(0x286c6c),
            shadow = c(0x003939),
            highlight = c(0x3a8d8d),
            barkColor = c(0x583838),
            barkShadow = c(0x440031),
            barkHighlight = c(0x00956f),
            leavesColor = c(0x008282),
            leavesHighlight = c(0x00b485),
            leavesShadow = c(0x006565),
        ) {
        override fun LayerListBuilder.doorCommonLayers() = copy(InvalidTask)

        override fun LayerListBuilder.trapdoor(commonLayers: AbstractImageTask) {
            layer("trapdoor1", highlight)
            layer("borderSolidThick", color)
            layer("borderSolid", highlight)
            layer("borderShortDashes", shadow)
            layer("waves", color)
            copy {
                layer("trapdoorHingesBig", STONE.shadow)
                layer("trapdoorHinges", STONE.highlight)
            }
        }

        override fun LayerListBuilder.doorBottom(commonLayers: AbstractImageTask) {
            background(color)
            layer("planksTopBorderVertical", shadow)
            layer("borderShortDashes", highlight)
            layer("waves", barkHighlight)
            layer("doorHingesBig", STONE.shadow)
            layer("doorHinges", STONE.highlight)
        }

        override fun LayerListBuilder.leaves() {
            background(leavesColor)
            layer("leaves3", leavesShadow)
            layer("leaves3a", leavesHighlight)
            layer("leaves3b", leavesHighlight)
            layer("borderSolid", leavesShadow)
            layer("borderShortDashes", leavesHighlight)
        }

        override fun LayerListBuilder.sapling() {
            layer("mushroomStem", barkShadow)
            layer("warpedFungusCap", leavesColor)
            layer("warpedFungusSpots", fungusSpotColor)
        }
    };

    override fun LayerListBuilder.bark() {
        background(barkColor)
        layer("borderSolid", barkShadow)
        layer("waves", barkHighlight)
    }

    override fun LayerListBuilder.strippedLogSide() {
        background(color)
        layer("borderSolid", shadow)
        layer("borderDotted", highlight)
    }

    override fun LayerListBuilder.logTop(strippedLogTop: AbstractImageTask) {
        background(color)
        copy {
            layer("ringsCentralBullseye", shadow)
            layer("rings2", highlight)
        }
        layer("borderSolid", barkColor)
        layer("borderShortDashes", barkShadow)
    }

    override fun LayerListBuilder.strippedLogTop(strippedLogSide: AbstractImageTask) {
        copy(strippedLogSide)
        copy {
            layer("ringsCentralBullseye", shadow)
            layer("rings2", highlight)
        }
    }

    override val logSynonym: String = "stem"
    override val leavesSynonym: String = "wart_block"
    override val saplingSynonym: String = "fungus"
}
