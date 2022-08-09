package io.github.pr0methean.ochd.materials.item

import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.c
import io.github.pr0methean.ochd.materials.DYES
import io.github.pr0methean.ochd.texturebase.Item
import io.github.pr0methean.ochd.texturebase.SingleTextureMaterial

val musicDiscColor = c(0x404040)
val musicDiscShadow = c(0x212121)
val musicDiscHighlight = c(0x515151)

@Suppress("unused", "EnumEntryName")
enum class MusicDisc(private val labelDyeName: String): SingleTextureMaterial, Item {
    FAR("red"),
    WAIT("green"),
    STRAD("brown"),
    MALL("blue"),
    CAT("purple"),
    PIGSTEP("cyan"),
    MELLOHI("light_gray"),
    `13`("pink"),
    BLOCKS("lime"),
    STAL("yellow"),
    WARD("light_blue"),
    `5`("magenta"),
    OTHERSIDE("orange"),
    CHIRP("gray"),
    `11`("") {
        override suspend fun LayerListBuilder.createTextureLayers() {
            layer("musicDiscBroken", musicDiscShadow)
            layer("musicDiscGrooveBroken", musicDiscHighlight)
        }
    };

    override val nameOverride: String?
        get() = "music_disc_$name"

    override suspend fun LayerListBuilder.createTextureLayers() {
        copy {
            layer("musicDisc", musicDiscColor)
            layer("musicDiscGroove", musicDiscShadow)
        }
        layer("musicDiscLabel", DYES[labelDyeName])
    }
}