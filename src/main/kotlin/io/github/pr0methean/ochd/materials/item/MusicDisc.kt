package io.github.pr0methean.ochd.materials.item

import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.c
import io.github.pr0methean.ochd.materials.DYES
import io.github.pr0methean.ochd.texturebase.Item
import io.github.pr0methean.ochd.texturebase.SingleTextureMaterial
import javafx.scene.paint.Color

val musicDiscColor: Color = c(0x404040)
val musicDiscShadow: Color = c(0x212121)
val musicDiscHighlight: Color = c(0x515151)

@Suppress("unused", "EnumEntryName")
enum class MusicDisc(private val labelDyeName: String): SingleTextureMaterial, Item {
    MUSIC_DISC_FAR("red"),
    MUSIC_DISC_WAIT("green"),
    MUSIC_DISC_STRAD("brown"),
    MUSIC_DISC_MALL("blue"),
    MUSIC_DISC_CAT("purple"),
    MUSIC_DISC_PIGSTEP("cyan"),
    MUSIC_DISC_MELLOHI("light_gray"),
    MUSIC_DISC_13("pink"),
    MUSIC_DISC_BLOCKS("lime"),
    MUSIC_DISC_STAL("yellow"),
    MUSIC_DISC_WARD("light_blue"),
    MUSIC_DISC_5("magenta"),
    MUSIC_DISC_OTHERSIDE("orange"),
    MUSIC_DISC_CHIRP("gray"),
    MUSIC_DISC_11("") {
        override fun LayerListBuilder.createTextureLayers() {
            layer("musicDiscBroken", musicDiscShadow)
            layer("musicDiscGrooveBroken", musicDiscHighlight)
        }
    };

    override fun LayerListBuilder.createTextureLayers() {
        copy {
            layer("musicDisc", musicDiscColor)
            layer("musicDiscGroove", musicDiscShadow)
        }
        layer("musicDiscLabel", DYES[labelDyeName]!!)
    }
}
