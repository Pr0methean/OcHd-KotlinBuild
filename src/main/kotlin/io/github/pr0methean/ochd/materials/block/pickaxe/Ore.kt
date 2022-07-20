package io.github.pr0methean.ochd.materials.block.pickaxe

import io.github.pr0methean.ochd.ImageProcessingContext
import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.c
import io.github.pr0methean.ochd.materials.block.pickaxe.OreBase.Companion.stoneExtremeHighlight
import io.github.pr0methean.ochd.tasks.OutputTask
import io.github.pr0methean.ochd.tasks.TextureTask
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
        highlight = c(0x494949)) {
        override fun oreBlock(ctx: ImageProcessingContext, oreBase: OreBase): TextureTask {
            if (oreBase == OreBase.DEEPSLATE) {
                return ctx.stack {
                    copy(OreBase.DEEPSLATE)
                    layer("coalBorder", stoneExtremeHighlight)
                    item()
                }
            }
            return super.oreBlock(ctx, oreBase)
        }
    },
    COPPER(
        color = c(0xe0734d),
        shadow = c(0x904931),
        highlight = c(0xff8268),
        needsRefining = true),
    IRON(
        color=c(0xd8af93),
        shadow=c(0xaf8e77),
        highlight=c(0xffc0aa),
        needsRefining = true,
        refinedColor = c(0xdcdcdc),
        refinedHighlight = Color.WHITE,
        refinedShadow = c(0xb0b0b0)),
    REDSTONE(
        color=Color.RED,
        shadow=c(0xca0000),
        highlight = c(0xff5e5e)
    ) {
        override fun LayerListBuilder.itemForOutput() {
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
        override fun outputTasks(ctx: ImageProcessingContext): Flow<OutputTask> = flow {
            emit(ctx.out("item/quartz") { ingot() })
            emit(ctx.out("block/nether_quartz_ore", ctx.stack {
                    copy(OreBase.NETHERRACK)
                copy {item()}
                }))
            emit(ctx.out("block/quartz_block_top") {
                background(color)
                layer("streaks", highlight)
                layer("borderSolid", shadow)
                layer("borderSolidTopLeft", highlight)
            })
            emit(ctx.out("block/quartz_block_bottom") {rawBlock()})
            emit(ctx.out("block/quartz_block_side") {block()})
        }

    },
    LAPIS(
        color=c(0x1855bd),
        shadow=c(0x00009c),
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
        color=c(0x1ed0d6),
        shadow=c(0x239698),
        highlight=c(0x77e7d1)
    ) {
        val extremeHighlight = c(0xd5ffff)
        override fun LayerListBuilder.item() {
            layer("diamond1", extremeHighlight)
            layer("diamond2", shadow)
        }

        override fun LayerListBuilder.block() {
            background(color)
            layer("streaks", highlight)
            copy {item()}
            layer("borderSolid", shadow)
            layer("borderSolidTopLeft", extremeHighlight)
        }
    },
    EMERALD(
        color=c(0x1c9829),
        shadow=c(0x007b18),
        highlight=c(0x1cdd62)
    ) {
        val extremeHighlight = c(0xd9ffeb)
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

    override fun outputTasks(ctx: ImageProcessingContext): Flow<OutputTask> = flow {
        substrates.forEach { oreBase ->
            emit(ctx.out("block/${oreBase.orePrefix}${name}_ore", oreBlock(ctx, oreBase)))
        }
        emit(ctx.out("block/${name}_block", ctx.stack { block() }))
        if (needsRefining) {
            emit(ctx.out("block/raw_${name}_block", ctx.stack { rawBlock() }))
            emit(ctx.out("item/raw_${name}", ctx.stack { rawOre() }))
            emit(ctx.out("item/${name}_ingot", ctx.stack { ingot() }))
        } else {
            emit(ctx.out("item/${itemNameOverride ?: name}", ctx.stack {itemForOutput()}))
        }
    }

    protected open fun oreBlock(
        ctx: ImageProcessingContext,
        oreBase: OreBase
    ) = ctx.stack {
        copy(oreBase)
        copy { item() }
    }
}