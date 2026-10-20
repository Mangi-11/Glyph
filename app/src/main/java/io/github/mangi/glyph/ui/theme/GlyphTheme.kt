package io.github.mangi.glyph.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import io.github.mangi.glyph.ui.share.LocalDarkTheme

@Composable
fun GlyphTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val themeConfig = ThemeConfig(
        colorMode = when {
            darkTheme -> 2
            else -> 1
        },
    )
    androidx.compose.runtime.CompositionLocalProvider(LocalDarkTheme provides darkTheme) {
        GlyphAppTheme(themeConfig = themeConfig, content = content)
    }
}
