package io.github.pr0methean.ochd.packedimage

import javafx.scene.paint.Color
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class ImageNodeKtTest {

    @Test
    fun alphaBlend() {
        assertEquals(Color.ORANGE, alphaBlend(Color.ORANGE, Color.TEAL))
        assertEquals(Color.ORANGE, alphaBlend(Color.ORANGE, Color.TRANSPARENT))
        assertEquals(Color.TEAL, alphaBlend(Color.TRANSPARENT, Color.TEAL))
        assertEquals(Color.TRANSPARENT, alphaBlend(Color.TRANSPARENT, Color.TRANSPARENT))
        val semiTransparentGray = Color(0.5, 0.5, 0.5, 0.5)
        assertEquals(Color.TEAL, alphaBlend(Color.TEAL, semiTransparentGray))
        assertEquals(Color(0.25, 0.25, 0.75, 1.0), alphaBlend(semiTransparentGray, Color.BLUE))
        assertEquals(Color(0.25, 0.75, 0.25, 1.0), alphaBlend(semiTransparentGray, Color.LIME))
        assertEquals(Color(0.75, 0.25, 0.25, 1.0), alphaBlend(semiTransparentGray, Color.RED))
        assertEquals(Color(0.5, 0.5, 0.5, 0.75), alphaBlend(semiTransparentGray, semiTransparentGray))
    }
}