package io.github.mangi.glyph.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Density

val LocalAppDarkMode = staticCompositionLocalOf { false }
val LocalAppMonetEnabled = staticCompositionLocalOf { false }
val LocalPlatformDensity = staticCompositionLocalOf<Density?> { null }
