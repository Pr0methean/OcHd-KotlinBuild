package io.github.pr0methean.ochd.materials.block.pickaxe

import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.TaskPlanningContext
import io.github.pr0methean.ochd.c
import io.github.pr0methean.ochd.materials.block.pickaxe.OreBase.DEEPSLATE
import io.github.pr0methean.ochd.materials.block.pickaxe.OreBase.NETHERRACK
import io.github.pr0methean.ochd.materials.block.pickaxe.OreBase.STONE
import io.github.pr0methean.ochd.tasks.AbstractImageTask
import io.github.pr0methean.ochd.tasks.PngOutputTask
import io.github.pr0methean.ochd.texturebase.ShadowHighlightMaterial
import javafx.scene.paint.Color
import java.util.*

private val overworldOreBases = EnumSet.of(STONE, DEEPSLATE)
private val netherOreBases = EnumSet.of(NETHERRACK)
private val allOreBases = EnumSet.allOf(OreBase::class.java)

@Suppress("unused")
enum class Ore(
    override val color: Color,
    override val shadow: Color,
    override val highlight: Color,
    val substrates: EnumSet<OreBase> = overworldOreBases,
    val needsRefining: Boolean = false,
    val itemNameOverride: String? = null,
    val refinedColor: Color = color,
    val refinedShadow: Color = shadow,
    val refinedHighlight: Color = highlight
): ShadowHighlightMaterial {
    COAL(
        color = c(0x2f2f2f),
        shadow = Color.BLACK,
        highlight = c(0x515151)
    ) {
        override fun oreBlock(
            ctx: TaskPlanningContext,
            oreBase: OreBase
        ): AbstractImageTask {
            if (oreBase == DEEPSLATE) {
                return ctx.stack {
                    copy(DEEPSLATE)
                    layer("coalBorder", refinedHighlight)
                    item()
                }
            }
            return super.oreBlock(ctx, oreBase)
        }

        override fun LayerListBuilder.block() {
            background(color)
            layer("streaks", refinedHighlight)
            layer("coalBorder", refinedHighlight)
            layer("coal", refinedShadow)
            layer("borderSolid", refinedShadow)
            layer("borderSolidTopLeft", refinedHighlight)
        }
    },
    COPPER(
        color = c(0xe0734d),
        shadow = c(0x915431),
        highlight = c(0xff8268),
        needsRefining = true
    ),
    IRON(
        color = c(0xd8af93),
        shadow = c(0xaf8e77),
        highlight = c(0xFFCDB2),
        needsRefining = true,
        refinedColor = c(0xdcdcdc),
        refinedHighlight = Color.WHITE,
        refinedShadow = c(0xaaaaaa)
    ),
    REDSTONE(
        color = Color.RED,
        shadow = c(0xca0000),
        highlight = c(0xff5e5e)
    ) {
        override fun LayerListBuilder.itemForOutput() {
            rawOre()
        }

        override fun oreBlock(ctx: TaskPlanningContext, oreBase: OreBase): AbstractImageTask {
            return if (oreBase == STONE) {
                ctx.stack {
                    copy(STONE)
                    layer("redstone", shadow)
                }
            } else super.oreBlock(ctx, oreBase)
        }
    },
    GOLD(
        color = Color.YELLOW,
        shadow = c(0xeb9d00),
        highlight = c(0xffffb5),
        needsRefining = true,
        substrates = allOreBases
    ),
    QUARTZ(
        color = c(0xe8e8de),
        shadow = c(0xb6a48e),
        highlight = Color.WHITE,
        substrates = netherOreBases
    ) {
        override fun outputTasks(ctx: TaskPlanningContext): Sequence<PngOutputTask> = sequence {
            yield(ctx.out({ ingot() }, "item/quartz"))
            yield(ctx.out(ctx.stack {
                copy(NETHERRACK)
                item() // don't need to copy since it's not used as the item form
            }, "block/nether_quartz_ore"))
            yield(ctx.out({
                background(color)
                layer("streaks", highlight)
                layer("borderSolid", shadow)
                layer("borderSolidTopLeft", highlight)
            }, "block/quartz_block_top"))
            yield(ctx.out({ rawBlock() }, "block/quartz_block_bottom"))
            yield(ctx.out({ block() }, "block/quartz_block_side"))
        }

    },
    LAPIS(
        color = c(0x0055bd),
        shadow = c(0x00009c),
        highlight = c(0x6995ff),
        itemNameOverride = "lapis_lazuli"
    ) {
        override fun LayerListBuilder.item() {
            layer("lapis", color)
            layer("lapisHighlight", highlight)
            layer("lapisShadow", shadow)
        }

        override fun LayerListBuilder.block() {
            background(highlight)
            layer("checksLarge", shadow)
            layer("checksSmall", color)
            layer("borderSolid", shadow)
            layer("borderSolidTopLeft", highlight)
        }
    },
    DIAMOND(
        color = c(0x20d3d3),
        shadow = c(0x209797),
        highlight = c(0x77e7d1)
    ) {
        private val extremeHighlight = c(0xd5ffff)
        override fun LayerListBuilder.item() {
            layer("diamond1", extremeHighlight)
            layer("diamond2", shadow)
        }

        override fun LayerListBuilder.block() {
            background(color)
            layer("streaks", highlight)
            copy { item() }
            layer("borderSolid", shadow)
            layer("borderSolidTopLeft", extremeHighlight)
        }
    },
    EMERALD(
        color = c(0x009829),
        shadow = c(0x007b18),
        highlight = c(0x00dd62)
    ) {
        private val extremeHighlight = c(0xd9ffeb)
        override fun LayerListBuilder.item() {
            layer("emeraldTopLeft", highlight)
            layer("emeraldBottomRight", shadow)
        }

        override fun LayerListBuilder.block() {
            background(highlight)
            layer("emeraldTopLeft", extremeHighlight)
            layer("emeraldBottomRight", shadow)
            layer("borderSolid", color)
            layer("borderSolidTopLeft", highlight)
        }
    };

    private val svgName = name.lowercase(Locale.ENGLISH)
    open fun LayerListBuilder.item() {
        layer(svgName, color)
    }

    open fun LayerListBuilder.block() {
        background(color)
        layer("streaks", refinedHighlight)
        layer(svgName, refinedShadow)
        layer("borderSolid", refinedShadow)
        layer("borderSolidTopLeft", refinedHighlight)
    }

    open fun LayerListBuilder.ingot() {
        layer("ingotMask", refinedColor)
        layer("ingotBorder", refinedShadow)
        layer("ingotBorderTopLeft", refinedHighlight)
        layer(svgName, shadow)
    }

    open fun LayerListBuilder.rawOre() {
        layer("bigCircle", shadow)
        layer(svgName, highlight)
    }

    open fun LayerListBuilder.rawBlock() {
        background(color)
        layer("checksSmall", highlight)
        layer(svgName, shadow)
    }

    open fun LayerListBuilder.itemForOutput() {
        item()
    }

    override fun outputTasks(ctx: TaskPlanningContext): Sequence<PngOutputTask> = sequence {
        substrates.forEach { oreBase ->
            yield(ctx.out(oreBlock(ctx, oreBase), "block/${oreBase.orePrefix}${name}_ore"))
        }
        yield(ctx.out(ctx.stack { block() }, "block/${name}_block"))
        if (needsRefining) {
            yield(ctx.out(ctx.stack { rawBlock() }, "block/raw_${name}_block"))
            yield(ctx.out(ctx.stack { rawOre() }, "item/raw_${name}"))
            yield(ctx.out(ctx.stack { ingot() }, "item/${name}_ingot"))
        } else {
            yield(ctx.out(ctx.stack { itemForOutput() }, "item/${itemNameOverride ?: name}"))
        }
    }

    protected open fun oreBlock(
        ctx: TaskPlanningContext,
        oreBase: OreBase
    ): AbstractImageTask = ctx.stack {
        copy(oreBase)
        copy { item() }
    }
}
