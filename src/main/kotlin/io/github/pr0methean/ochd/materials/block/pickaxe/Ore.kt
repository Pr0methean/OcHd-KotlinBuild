package io.github.pr0methean.ochd.materials.block.pickaxe

import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.OutputTaskBuilder
import io.github.pr0methean.ochd.c
import io.github.pr0methean.ochd.materials.block.pickaxe.OreBase.DEEPSLATE
import io.github.pr0methean.ochd.materials.block.pickaxe.OreBase.NETHERRACK
import io.github.pr0methean.ochd.materials.block.pickaxe.OreBase.STONE
import io.github.pr0methean.ochd.tasks.AbstractImageTask
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
            ctx: OutputTaskBuilder,
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

        override fun oreBlock(ctx: OutputTaskBuilder, oreBase: OreBase): AbstractImageTask {
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
        override suspend fun OutputTaskBuilder.outputTasks() {
            out("item/quartz") { ingot() }
            out("block/nether_quartz_ore") {
                copy(NETHERRACK)
                item() // don't need to copy since it's not used to create the item form or any other texture
            }
            out("block/quartz_block_top") {
                background(color)
                layer("streaks", highlight)
                layer("borderSolid", shadow)
                layer("borderSolidTopLeft", highlight)
            }
            out("block/quartz_block_bottom") { rawBlock() }
            out("block/quartz_block_side") { block() }
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

    open fun LayerListBuilder.itemForOutput(): Unit = item()

    override suspend fun OutputTaskBuilder.outputTasks() {
        substrates.forEach { oreBase ->
            out("block/${oreBase.orePrefix}${this@Ore.name}_ore", oreBlock(this@outputTasks, oreBase))
        }
        out("block/${this@Ore.name}_block") { block() }
        if (needsRefining) {
            out("block/raw_${this@Ore.name}_block") { rawBlock() }
            out("item/raw_${this@Ore.name}") { rawOre() }
            out("item/${this@Ore.name}_ingot") { ingot() }
        } else {
            out("item/${itemNameOverride ?: this@Ore.name}") { itemForOutput() }
        }
    }

    protected open fun oreBlock(
        ctx: OutputTaskBuilder,
        oreBase: OreBase
    ): AbstractImageTask = ctx.stack {
        copy(oreBase)
        copy { item() }
    }
}
