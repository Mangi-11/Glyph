package io.github.mangi.glyph.hook.systemui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LockScreenCapsuleColorPolicyTest {

    @Test
    fun `rule glyphs on the island stay white outlines`() {
        assertTrue(LockScreenCapsuleColorPolicy.shouldApplyWhiteGlyphTint(isColorable = true))
    }

    @Test
    fun `desktop theme assets keep original colors on aggregate island icons`() {
        assertFalse(LockScreenCapsuleColorPolicy.shouldApplyWhiteGlyphTint(isColorable = false))
    }
}
