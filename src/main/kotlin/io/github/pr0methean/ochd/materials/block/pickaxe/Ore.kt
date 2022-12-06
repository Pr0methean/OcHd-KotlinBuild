package io.github.pr0methean.ochd.materials.block.pickaxe

import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.TaskPlanningContext
import io.github.pr0methean.ochd.c
import io.github.pr0methean.ochd.tasks.ImageTask
import io.github.pr0methean.ochd.tasks.OutputTask
import io.github.pr0methean.ochd.texturebase.ShadowHighlightMaterial
import javafx.scene.paint.Color
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.*

private val OVERWORLD = EnumSet.of(OreBase.STONE, OreBase.DEEPSLATE)
private val NETHER = EnumSet.of(OreBase.NETHERRACK)
private val BOTH = EnumSet.allOf(OreBase::class.java)

@Suppress("unused")
enum class Ore(
    override val color: Color,
    override val shadow: Color,
    override val highlight: Color,
    val substrates: EnumSet<OreBase> = OVERWORLD,
    val needsRefining: Boolean = false,
    val itemNameOverride: String? = null,
    val refinedColor: Color = color,
    val refinedShadow: Color = shadow,
    val refinedHighlight: Color = highlight
): ShadowHighlightMaterial {
    COAL(
        color = c(0x2f2f2f),
        shadow = Color.BLACK,
        highlight = c(0x515151)) {
        override suspend fun oreBlock(ctx: TaskPlanningContext, oreBase: OreBase): ImageTask {
            if (oreBase == OreBase.DEEPSLATE) {
                return ctx.stack {
                    copy(OreBase.DEEPSLATE)
                    layer("coalBorder", refinedHighlight)
                    item()
                }
            }
            return super.oreBlock(ctx, oreBase)
        }

        override suspend fun LayerListBuilder.block() {
            background(color)
            layer("streaks", refinedHighlight)
            layer("coal", refinedShadow)
            layer("coalBorder", refinedHighlight)
            layer("borderSolid", refinedShadow)
            layer("borderSolidTopLeft", refinedHighlight)
        }
    },
    COPPER(
        color = c(0xe0734d),
        shadow = c(0x915431),
        highlight = c(0xff8268),
        needsRefining = true),
    IRON(
        color=c(0xd8af93),
        shadow=c(0xaf8e77),
        highlight=c(0xFFCDB2),
        needsRefining = true,
        refinedColor = c(0xdcdcdc),
        refinedHighlight = Color.WHITE,
        refinedShadow = c(0xaaaaaa)),
    REDSTONE(
        color=Color.RED,
        shadow=c(0xca0000),
        highlight = c(0xff5e5e)
    ) {
        override suspend fun LayerListBuilder.itemForOutput() {
            rawOre()
        }
    },
    GOLD(
        color=Color.YELLOW,
        shadow=c(0xeb9d00),
        highlight=c(0xffffb5),
        needsRefining = true,
        substrates = BOTH
    ),
    QUARTZ(
        color=c(0xe8e8de),
        shadow=c(0xb6a48e),
        highlight = Color.WHITE,
        substrates = NETHER
    ) {
        override suspend fun outputTasks(ctx: TaskPlanningContext): Flow<OutputTask> = flow {
            emit(ctx.out({ ingot() }, "item/quartz"))
            emit(ctx.out(ctx.stack {
                    copy(OreBase.NETHERRACK)
                copy {item()}
                }, "block/nether_quartz_ore"))
            emit(ctx.out({
                background(color)
                layer("streaks", highlight)
                layer("borderSolid", shadow)
                layer("borderSolidTopLeft", highlight)
            }, "block/quartz_block_top"))
            emit(ctx.out({rawBlock()}, "block/quartz_block_bottom"))
            emit(ctx.out({block()}, "block/quartz_block_side"))
        }

    },
    LAPIS(
        color=c(0x0055bd),
        shadow=c(0x00009c),
        highlight = c(0x6995ff),
        itemNameOverride = "lapis_lazuli"
    ) {
        override suspend fun LayerListBuilder.item() {
            layer("lapis", color)
            layer("lapisHighlight", highlight)
            layer("lapisShadow", shadow)
        }

        override suspend fun LayerListBuilder.block() {
            background(highlight)
            layer("checksLarge", shadow)
            layer("checksSmall", color)
            layer("borderSolid", shadow)
            layer("borderSolidTopLeft", highlight)
        }
    },
    DIAMOND(
        color=c(0x20d3d3),
        shadow=c(0x209797),
        highlight=c(0x77e7d1)
    ) {
        private val extremeHighlight = c(0xd5ffff)
        override suspend fun LayerListBuilder.item() {
            layer("diamond1", extremeHighlight)
            layer("diamond2", shadow)
        }

        override suspend fun LayerListBuilder.block() {
            background(color)
            layer("streaks", highlight)
            copy {item()}
            layer("borderSolid", shadow)
            layer("borderSolidTopLeft", extremeHighlight)
        }
    },
    EMERALD(
        color=c(0x009829),
        shadow=c(0x007b18),
        highlight=c(0x00dd62)
    ) {
        private val extremeHighlight = c(0xd9ffeb)
        override suspend fun LayerListBuilder.item() {
            layer("emeraldTopLeft", highlight)
            layer("emeraldBottomRight", shadow)
        }

        override suspend fun LayerListBuilder.block() {
            background(highlight)
            layer("emeraldTopLeft", extremeHighlight)
            layer("emeraldBottomRight", shadow)
            layer("borderSolid", color)
            layer("borderSolidTopLeft", highlight)
        }
    };
    private val svgName = name.lowercase(Locale.ENGLISH)
    open suspend fun LayerListBuilder.item() {
        layer(svgName, color)
    }

    open suspend fun LayerListBuilder.block() {
        background(color)
        layer("streaks", refinedHighlight)
        layer(svgName, refinedShadow)
        layer("borderSolid", refinedShadow)
        layer("borderSolidTopLeft", refinedHighlight)
    }
    open suspend fun LayerListBuilder.ingot() {
        layer("ingotMask", refinedColor)
        layer("ingotBorder", refinedShadow)
        layer("ingotBorderTopLeft", refinedHighlight)
        layer(svgName, shadow)
    }
    open suspend fun LayerListBuilder.rawOre() {
        layer("bigCircle", shadow)
        layer(svgName, highlight)
    }
    open suspend fun LayerListBuilder.rawBlock() {
        background(color)
        layer("checksSmall", highlight)
        layer(svgName, shadow)
    }

    open suspend fun LayerListBuilder.itemForOutput() {
        item()
    }

    override suspend fun outputTasks(ctx: TaskPlanningContext): Flow<OutputTask> = flow {
        substrates.forEach { oreBase ->
            emit(ctx.out(oreBlock(ctx, oreBase), "block/${oreBase.orePrefix}${name}_ore"))
        }
        emit(ctx.out(ctx.stack { block() }, "block/${name}_block"))
        if (needsRefining) {
            emit(ctx.out(ctx.stack { rawBlock() }, "block/raw_${name}_block"))
            emit(ctx.out(ctx.stack { rawOre() }, "item/raw_${name}"))
            emit(ctx.out(ctx.stack { ingot() }, "item/${name}_ingot"))
        } else {
            emit(ctx.out(ctx.stack {itemForOutput()}, "item/${itemNameOverride ?: name}"))
        }
    }

    protected open suspend fun oreBlock(
        ctx: TaskPlanningContext,
        oreBase: OreBase
    ): ImageTask = ctx.stack {
        copy(oreBase)
        copy { item() }
    }
}
