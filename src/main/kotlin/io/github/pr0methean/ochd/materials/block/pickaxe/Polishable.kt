package io.github.pr0methean.ochd.materials.block.pickaxe

/*
enum class Polishable(
    override val color: Paint,
    override val shadow: Paint,
    override val highlight: Paint
): ShadowHighlightMaterial {
    ANDESITE(c(0x8b8b8b),c(0x686868)),
    DIORITE,
    GRANITE,
    BLACKSTONE;

    abstract fun LayerListBuilder.createTextureLayersBase()
    fun LayerListBuilder.createBorderRaw() {
        layer("borderDotted", shadow)
    }
    fun LayerListBuilder.createBorderPolished() {
        layer("borderSolid", shadow)
        layer("borderSolidTopLeft", highlight)
    }

    override fun outputTasks(ctx: ImageProcessingContext): Iterable<OutputTask> {
        val baseTask = ctx.stack {createTextureLayersBase()}
        return listOf(
            ctx.out("block/$name", ctx.stack {
                add(baseTask)
                createBorderRaw()
            }),
            ctx.out("block/polished_$name", ctx.stack {
                add(baseTask)
                createBorderPolished()
            })
        )
    }
}
 */
/*
andesite_h='a9a99a'
andesite='8b8b8b'
andesite_s='686868'
diorite_h='ffffff'
diorite='bfbfbf'
diorite_s='7b7b7b'
granite_h='e3c0af'
granite='9f6b58'
granite_s='5f4034'
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