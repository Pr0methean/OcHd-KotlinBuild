package io.github.pr0methean.ochd.materials.block.axe

import io.github.pr0methean.ochd.ImageProcessingContext
import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.c
import io.github.pr0methean.ochd.materials.block.pickaxe.OreBase.STONE
import io.github.pr0methean.ochd.materials.block.shovel.DirtGroundCover
import io.github.pr0methean.ochd.tasks.ImageTask
import io.github.pr0methean.ochd.tasks.OutputTask
import io.github.pr0methean.ochd.texturebase.ShadowHighlightMaterial
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

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
    suspend fun LayerListBuilder.bark()
    suspend fun LayerListBuilder.strippedLogSide()
    suspend fun LayerListBuilder.logTop()
    suspend fun LayerListBuilder.strippedLogTop()
    suspend fun LayerListBuilder.trapdoor()
    suspend fun LayerListBuilder.doorTop(doorKnob: ImageTask)
    suspend fun LayerListBuilder.doorBottom()

    suspend fun LayerListBuilder.leaves()
    suspend fun LayerListBuilder.sapling()

    override suspend fun outputTasks(ctx: ImageProcessingContext): Flow<OutputTask> = flow {
        val doorKnob = ctx.stack {
            layer("doorKnob", STONE.highlight)
            layer("doorKnobShadow", STONE.shadow)
        }
        doorKnob.enableCaching()
        emit(ctx.out(ctx.stack { bark() }, "block/${name}_${logSynonym}"))
        emit(ctx.out(ctx.stack { strippedLogSide() }, "block/stripped_${name}_${logSynonym}"))
        emit(ctx.out(ctx.stack { strippedLogTop() }, "block/stripped_${name}_${logSynonym}_top"))
        emit(ctx.out(ctx.stack { logTop() }, "block/${name}_${logSynonym}_top"))
        emit(ctx.out(ctx.stack { trapdoor() }, "block/${name}_trapdoor"))
        emit(ctx.out(ctx.stack { doorTop(doorKnob) }, "block/${name}_door_top"))
        emit(ctx.out(ctx.stack { doorBottom() }, "block/${name}_door_bottom"))
        emit(ctx.out(ctx.stack { leaves() }, "block/${name}_${leavesSynonym}"))
        emit(ctx.out(ctx.stack { sapling() }, "block/${name}_${saplingSynonym}"))
        emit(ctx.out(ctx.stack { planks() }, "block/${name}_planks"))
    }

    suspend fun LayerListBuilder.planks() {
        background(color)
        layer("waves", highlight)
        layer("planksTopBorder", shadow)
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
        color = Color.rgb(0xad, 0x5d, 0x32),
        highlight = Color.rgb(0xc2, 0x6d, 0x3f),
        shadow = Color.rgb(0x8f, 0x4c, 0x2a),
        barkColor = c(0x696259),
        barkHighlight = c(0x8d8477),
        barkShadow = c(0x504b40),
    ) {
        override suspend fun LayerListBuilder.trapdoor() {
            copy {
                layer("borderSolidThick", color)
                layer("borderSolid", highlight)
                layer("bigDiamond", shadow)
            }
            layer("trapdoorHingesBig", STONE.shadow)
            layer("trapdoorHinges", STONE.highlight)
        }

        override suspend fun LayerListBuilder.doorTop(doorKnob: ImageTask) {
            copy { doorBottom() }
            copy(doorKnob)
        }

        override suspend fun LayerListBuilder.doorBottom() {
            copy {
                layer("borderSolidThick", color)
                layer("borderSolid", highlight)
                layer("bigDiamond", shadow)
            }
            layer("x", color)
            layer("doorHingesBig", STONE.shadow)
            layer("doorHinges", STONE.highlight)
        }

        override suspend fun LayerListBuilder.leaves() {
            layer("leaves1", leavesShadow)
            layer("leaves1a", leavesHighlight)
        }

        override suspend fun LayerListBuilder.sapling() {
            layer("saplingStem", barkColor)
            layer("acaciaSapling", c(0x6c9e38))
            layer("acaciaSapling2", c(0xc9d7a5))
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
        override suspend fun LayerListBuilder.trapdoor() {
            copy {
                background(Color.WHITE)
                layer("borderSolidExtraThick", color)
                layer("craftingGrid", highlight)
                layer("borderSolid", shadow)
            }
            layer("trapdoorHingesBig", STONE.shadow)
        }

        override suspend fun LayerListBuilder.doorTop(doorKnob: ImageTask) {
            copy {
                background(Color.WHITE)
                layer("borderSolidExtraThick", color)
                layer("craftingGrid", highlight)
                layer("borderSolid", shadow)
            }
            layer("doorHingesBig", STONE.shadow)
            copy(doorKnob)
        }

        override suspend fun LayerListBuilder.doorBottom() {
            background(highlight)
            layer("borderSolidExtraThick", color)
            layer("craftingGrid", shadow)
            layer("borderSolid", shadow)
            layer("doorHingesBig", STONE.shadow)
        }

        override suspend fun LayerListBuilder.leaves() {
            layer("leaves2", leavesHighlight)
            layer("leaves2a", leavesShadow)
        }

        override suspend fun LayerListBuilder.sapling() {
            layer("saplingStem", barkColor)
            layer("flowerStemBottomBorder", barkShadow)
            layer("saplingLeaves", c(0x6c9e38))
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
        override suspend fun LayerListBuilder.trapdoor() {
            copy {
                background(color)
                layer("2x2BottomRight", highlight)
                layer("2x2TopLeft", shadow)
                layer("borderShortDashes", color)
            }
            layer("trapdoorHingesBig", STONE.highlight)
        }

        override suspend fun LayerListBuilder.doorTop(doorKnob: ImageTask) {
            copy {doorBottom()}
            copy(doorKnob)
        }

        override suspend fun LayerListBuilder.doorBottom() {
            copy {
                background(color)
                layer("2x2BottomRight", highlight)
                layer("2x2TopLeft", shadow)
                layer("borderShortDashes", color)
            }
            layer("doorHingesBig", STONE.highlight)
        }

        override suspend fun LayerListBuilder.leaves() {
            layer("leaves3", leavesShadow)
            layer("leaves3a", leavesHighlight)
        }

        override suspend fun LayerListBuilder.sapling() {
            layer("saplingStem", barkColor)
            layer("bigCircle", c(0x005200))
            layer("bigCircleTwoQuarters", c(0x57ad3f))
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
        override suspend fun LayerListBuilder.trapdoor() {
            layer("trapdoor2", color)
            layer("borderSolid", shadow)
            layer("borderShortDashes", highlight)
            layer("trapdoorHingesBig", STONE.shadow)
            layer("trapdoorHinges", STONE.color)
        }

        override suspend fun LayerListBuilder.doorTop(doorKnob: ImageTask) {
            layer("trapdoor2", color)
            layer("borderShortDashes", highlight)
            copy {
                layer("doorHingesBig", STONE.shadow)
                layer("doorHinges", STONE.color)
            }
            copy(doorKnob)
        }

        override suspend fun LayerListBuilder.doorBottom() {
            background(color)
            layer("waves", highlight)
            layer("planksTopBorderVertical", shadow)
            layer("borderSolid", color)
            layer("borderShortDashes", highlight)
            copy {
                layer("doorHingesBig", STONE.shadow)
                layer("doorHinges", STONE.color)
            }
        }

        override suspend fun LayerListBuilder.leaves() {
            layer("leaves6", leavesHighlight)
            layer("leaves6a", leavesShadow)
        }

        override suspend fun LayerListBuilder.sapling() {
            layer("saplingStem", barkColor)
            layer("acaciaSapling", c(0x378020))
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
        override val saplingSynonym: String = "propagule"
        override suspend fun LayerListBuilder.trapdoor() {
            layer("ringsHole", color)
            copy {
                layer("rings2", shadow)
                layer("borderDotted", highlight)
            }
            layer("trapdoorHingesBig", STONE.highlight)
            layer("trapdoorHinges", STONE.shadow)
        }

        override suspend fun LayerListBuilder.doorTop(doorKnob: ImageTask) {
            copy {doorBottom()}
            copy(doorKnob)
        }

        override suspend fun LayerListBuilder.doorBottom() {
            background(color)
            copy {
                layer("rings2", shadow)
                layer("borderDotted", highlight)
            }
            layer("doorHingesBig", STONE.highlight)
            layer("doorHinges", STONE.shadow)
        }

        override suspend fun LayerListBuilder.leaves() {
            layer("leaves5", leavesHighlight)
            layer("leaves5a", leavesColor)
            layer("leaves5b", leavesShadow)
        }

        override suspend fun LayerListBuilder.sapling() {
            layer("mangrovePropagule", c(0x4aa54a))
            layer("flowerStemBottomBorder", c(0x748241))
        }
    },
    SPRUCE(
        color = c(0x70522e),
        highlight = c(0x886539),
        shadow = c(0x5a4424),
        barkColor = c(0x3b2700),
        barkHighlight = c(0x553a1f),
        barkShadow = c(0x2e1c00)
    ) {
        override suspend fun LayerListBuilder.trapdoor() {
            background(shadow)
            layer("planksTopVertical", color)
            layer("borderSolidThick", shadow)
            layer("borderLongDashes", highlight)
            layer("trapdoorHingesBig", STONE.color)
            layer("trapdoorHinges", STONE.shadow)
        }

        override suspend fun LayerListBuilder.doorTop(doorKnob: ImageTask) {
            copy {doorBottom()}
            copy(doorKnob)
        }

        override suspend fun LayerListBuilder.doorBottom() {
            copy {planks()}
            layer("doorHingesBig", STONE.color)
            layer("doorHinges", STONE.shadow)
        }

        override suspend fun LayerListBuilder.leaves() {
            layer("leaves3", leavesHighlight)
            layer("leaves3b", leavesShadow)
        }

        override suspend fun LayerListBuilder.sapling() {
            layer("saplingStem", barkHighlight)
            layer("spruceSapling", c(0x2e492e))
        }
    },
    OAK(
        color = c(0xaf8f55),
        highlight = c(0xc29d62),
        shadow = c(0x7e6237),
        barkColor = c(0x745a36),
        barkHighlight = c(0x987849),
        barkShadow = c(0x4c3d26)
    ) {
        override suspend fun LayerListBuilder.trapdoor() {
            layer("cross", color)
            copy {
                layer("borderSolidThick", color)
                layer("borderSolid", shadow)
            }
            layer("borderLongDashes", highlight)
            layer("trapdoorHingesBig", STONE.color)
            layer("trapdoorHinges", STONE.highlight)
        }

        override suspend fun LayerListBuilder.doorTop(doorKnob: ImageTask) {
            copy {
                layer("borderSolidThick", color)
                layer("borderSolid", shadow)
            }
            layer("craftingSide", shadow)
            layer("cross", shadow)
            copy(doorKnob)
            copy {
                layer("doorHingesBig", STONE.color)
                layer("doorHinges", STONE.highlight)
            }
        }

        override suspend fun LayerListBuilder.doorBottom() {
            background(color)
            layer("waves", highlight)
            layer("cross", shadow)
            layer("borderSolid", shadow)
            copy {
                layer("doorHingesBig", STONE.color)
                layer("doorHinges", STONE.highlight)
            }
        }

        override suspend fun LayerListBuilder.leaves() {
            layer("leaves4", leavesShadow)
            layer("leaves4a", leavesHighlight)
        }

        override suspend fun LayerListBuilder.sapling() {
            layer("saplingStem", barkColor)
            layer("bigCircle", c(0x006500))
            layer("mushroomSpots", c(0x57ad3f))
        }
    };


    override suspend fun LayerListBuilder.bark() {
        background(barkColor)
        layer("borderSolid", barkShadow)
        layer("borderDotted", barkHighlight)
        layer("zigzagSolid", barkShadow)
        layer("zigzagSolid2", barkHighlight)
    }

    override suspend fun LayerListBuilder.strippedLogSide() {
        background(color)
        layer("borderSolid", shadow)
        layer("borderShortDashes", highlight)
    }

    override suspend fun LayerListBuilder.logTop() {
        copy {strippedLogTop()}
        layer("borderSolid", barkColor)
        layer("borderDotted", barkShadow)
    }

    override suspend fun LayerListBuilder.strippedLogTop() {
        copy { strippedLogSide() }
        layer("ringsCentralBullseye", highlight)
        layer("rings", shadow)
    }

    override val logSynonym = "log"
    override val leavesSynonym = "leaves"
    override val saplingSynonym = "sapling"

    // Like grass, leaves are stored as gray and colorized in real time based on the biome
    override val leavesColor = DirtGroundCover.GRASS_BLOCK.color
    override val leavesHighlight = DirtGroundCover.GRASS_BLOCK.highlight
    override val leavesShadow = DirtGroundCover.GRASS_BLOCK.shadow
}

private val fungusSpotColor = c(0xff6500)
@Suppress("unused")
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
            barkShadow = c(0x442131),
            barkHighlight = c(0xb10000),
            leavesColor = c(0x7b0000),
            leavesShadow = c(0x5a0000),
            leavesHighlight = c(0xac2020),
        ) {
            override suspend fun LayerListBuilder.trapdoor() {
                layer("borderSolidThick", color)
                layer("trapdoor1", shadow)
                layer("borderShortDashes", highlight)
                copy {
                    layer("zigzagSolid2", highlight)
                    layer("zigzagSolid", shadow)
                }
                layer("trapdoorHingesBig", STONE.highlight)
                layer("trapdoorHinges", STONE.shadow)
            }

            override suspend fun LayerListBuilder.doorTop(doorKnob: ImageTask) {
                copy {doorBottom()}
                copy(doorKnob)
            }

            override suspend fun LayerListBuilder.doorBottom() {
                background(color)
                layer("planksTopBorderVertical", shadow)
                layer("borderShortDashes", highlight)
                copy {
                    layer("zigzagSolid2", barkHighlight)
                    layer("zigzagSolid", shadow)
                }
                layer("doorHingesBig", STONE.highlight)
                layer("doorHinges", STONE.shadow)
            }

            override suspend fun LayerListBuilder.leaves() {
                background(leavesColor)
                layer("leaves6", leavesShadow)
                layer("leaves6a", leavesHighlight)
                layer("borderRoundDots", leavesHighlight)
            }
            override suspend fun LayerListBuilder.sapling() {
                layer("mushroomStem", barkShadow)
                layer("mushroomCapRed", leavesColor)
                layer("crimsonFungusSpots", fungusSpotColor)
            }
        }, WARPED(
            color = c(0x287067),
            shadow = c(0x1e4340),
            highlight = c(0x3a8e8c),
            barkColor = c(0x562c3e),
            barkShadow = c(0x442131),
            barkHighlight = c(0x00956f),
            leavesColor = c(0x008282),
            leavesHighlight = c(0x00b485),
            leavesShadow = c(0x006565),
        ) {
        override suspend fun LayerListBuilder.trapdoor() {
            layer("trapdoor1", highlight)
            layer("borderSolidThick", color)
            layer("borderSolid", highlight)
            layer("borderShortDashes", shadow)
            layer("waves", color)
            layer("trapdoorHingesBig", STONE.shadow)
            layer("trapdoorHinges", STONE.highlight)
        }

        override suspend fun LayerListBuilder.doorTop(doorKnob: ImageTask) {
            copy {doorBottom()}
            copy(doorKnob)
        }

        override suspend fun LayerListBuilder.doorBottom() {
            background(color)
            layer("planksTopBorderVertical", shadow)
            layer("borderShortDashes", highlight)
            layer("waves", barkHighlight)
            layer("doorHingesBig", STONE.shadow)
            layer("doorHinges", STONE.highlight)
        }

        override suspend fun LayerListBuilder.leaves() {
            background(leavesColor)
            layer("leaves3", leavesShadow)
            layer("leaves3a", leavesHighlight)
            layer("leaves3b", leavesHighlight)
            layer("borderSolid", leavesShadow)
            layer("borderShortDashes", leavesHighlight)
        }

        override suspend fun LayerListBuilder.sapling() {
            layer("mushroomStem", barkShadow)
            layer("warpedFungusCap", leavesColor)
            layer("warpedFungusSpots", fungusSpotColor)
        }
    };

    override suspend fun LayerListBuilder.bark() {
        background(barkColor)
        layer("borderSolid", barkShadow)
        layer("waves", barkHighlight)
    }

    override suspend fun LayerListBuilder.strippedLogSide() {
        background(color)
        layer("borderSolid", shadow)
        layer("borderDotted", highlight)
    }

    override suspend fun LayerListBuilder.logTop() {
        copy {strippedLogTop()}
        layer("borderSolid", barkColor)
        layer("borderDotted", barkShadow)
    }

    override suspend fun LayerListBuilder.strippedLogTop() {
        copy {strippedLogSide()}
        layer("ringsCentralBullseye", shadow)
        layer("rings2", highlight)
    }

    override val logSynonym = "stem"
    override val leavesSynonym = "wart_block"
    override val saplingSynonym = "fungus"
}
