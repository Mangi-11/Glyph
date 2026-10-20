package io.github.mangi.glyph.hook.systemui

/**
 * Lock-screen island coloring.
 *
 * Rule / placeholder glyphs are alpha masks and must be drawn white on the dark capsule.
 * Desktop-theme assets are full-color launcher icons; SRC_IN white turns them into solid blocks.
 */
internal object LockScreenCapsuleColorPolicy {

    fun shouldApplyWhiteGlyphTint(isColorable: Boolean): Boolean = isColorable
}
