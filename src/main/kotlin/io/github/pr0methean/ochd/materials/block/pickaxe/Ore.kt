package io.github.pr0methean.ochd.materials.block.pickaxe

import io.github.pr0methean.ochd.ImageProcessingContext
import io.github.pr0methean.ochd.LayerList
import io.github.pr0methean.ochd.c
import io.github.pr0methean.ochd.tasks.OutputTask
import io.github.pr0methean.ochd.texturebase.ShadowHighlightMaterial
import io.github.pr0methean.ochd.texturebase.copy
import io.github.pr0methean.ochd.texturebase.group
import javafx.scene.paint.Color
import java.util.*

private val OVERWORLD = EnumSet.of(OreBase.STONE, OreBase.DEEPSLATE)
private val NETHER = EnumSet.of(OreBase.NETHERRACK)
private val BOTH = EnumSet.allOf(OreBase::class.java)

val ORES = group<Ore>()
enum class Ore(
    override val color: Color,
    override val shadow: Color,
    override val highlight: Color,
    val substrates: EnumSet<OreBase> = OVERWORLD,
    val needsRefining: Boolean = false,
    val itemNameOverride: String? = null
): ShadowHighlightMaterial {
    COAL(
        color = c(0x2f2f2f),
        shadow = Color.BLACK,
        highlight = c(0x494949)),
    COPPER(
        color = c(0xe0734d),
        shadow = c(0x904931),
        highlight = c(0xff8268),
        needsRefining = true),
    IRON(
        color=c(0xd8af93),
        shadow=c(0xaf8e77),
        highlight=c(0xffc0aa),
        needsRefining = true),
    REDSTONE(
        color=Color.RED,
        shadow=c(0xca0000),
        highlight = c(0xff5e5e)
    ) {
        override fun LayerList.itemForOutput() {
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
        color=c(0xeae5de),
        shadow=c(0xb6a48e),
        highlight = Color.WHITE,
        substrates = NETHER
    ) {
        override fun outputTasks(ctx: ImageProcessingContext): Iterable<OutputTask> {
            val output = mutableListOf<OutputTask>()
            output.add(ctx.out("item/quartz") { ingot() })
            output.add(ctx.out("block/nether_quartz_ore", ctx.stack {
                    copy(OreBase.NETHERRACK)
                    copy {item()}
                }))
            output.add(ctx.out("block/quartz_block_top") {
                background(color)
                layer("streaks", highlight)
                layer("borderSolidTopLeft", highlight)
                layer("borderSolidBottomRight", shadow)
            })
            output.add(ctx.out("block/quartz_block_bottom") {rawBlock()})
            output.add(ctx.out("block/quartz_block_side") {block()})
            return output
        }

    },
    LAPIS(
        color=c(0x1855bd),
        shadow=c(0x00009c),
        highlight = c(0x6995ff),
        itemNameOverride = "lapis_lazuli"
    ) {
        override fun LayerList.item() {
            layer("lapis", color)
            layer("lapisHighlight", highlight)
            layer("lapisShadow", shadow)
        }

        override fun LayerList.block() {
            background(highlight)
            layer("checksLarge", shadow)
            layer("checksSmall", color)
            layer("borderSolidTopLeft", highlight)
            layer("borderSolidBottomRight", shadow)
        }
    },
    DIAMOND(
        color=c(0x1ed0d6),
        shadow=c(0x239698),
        highlight=c(0x77e7d1)
    ) {
        val extremeHighlight = c(0xd5ffff)
        override fun LayerList.item() {
            layer("diamond1", extremeHighlight)
            layer("diamond2", shadow)
        }

        override fun LayerList.block() {
            background(color)
            layer("streaks", highlight)
            copy {item()}
            layer("borderSolidTopLeft", extremeHighlight)
            layer("borderSolidBottomRight", shadow)
        }
    },
    EMERALD(
        color=c(0x1c9829),
        shadow=c(0x007b18),
        highlight=c(0x1cdd62)
    ) {
        val extremeHighlight = c(0xd9ffeb)
        override fun LayerList.item() {
            layer("emeraldTopLeft", highlight)
            layer("emeraldBottomRight", shadow)
        }

        override fun LayerList.block() {
            background(highlight)
            layer("emeraldTopLeft", extremeHighlight)
            layer("emeraldBottomRight", shadow)
            layer("borderSolid", color)
            layer("borderSolidTopLeft", highlight)
        }
    };
    open fun LayerList.item() {
        layer(name.lowercase(Locale.ENGLISH), color)
    }

    open fun LayerList.block() {
        background(color)
        layer("streaks", highlight)
        copy {item()}
        layer("borderSolidTopLeft", highlight)
        layer("borderSolidBottomRight", shadow)
    }
    open fun LayerList.ingot() {
        layer("ingotMask", color)
        layer("ingotBorder", shadow)
        layer("ingotBorderTopLeft", highlight)
        layer(name, shadow)
    }
    open fun LayerList.rawOre() {
        layer("bigCircle", shadow)
        layer(name, highlight)
    }
    open fun LayerList.rawBlock() {
        background(color)
        layer("checksSmall", highlight)
        layer(name, shadow)
    }

    open fun LayerList.itemForOutput() {
        item()
    }

    override fun outputTasks(ctx: ImageProcessingContext): Iterable<OutputTask> {
        val output = mutableListOf<OutputTask>()
        substrates.forEach { oreBase ->
            output.add(ctx.out("block/${oreBase.orePrefix}${name}_ore", ctx.stack {
                copy(oreBase)
                copy {item()}
            }))
        }
        output.add(ctx.out("block/${name}_block", ctx.stack { block() }))
        if (needsRefining) {
            output.add(ctx.out("block/raw_${name}_block", ctx.stack { rawBlock() }))
            output.add(ctx.out("item/raw_${name}", ctx.stack { rawOre() }))
            output.add(ctx.out("item/${name}_ingot", ctx.stack { ingot() }))
        } else {
            output.add(ctx.out("item/${itemNameOverride ?: name}", ctx.stack {itemForOutput()}))
        }
        return output
    }
}