package io.github.mangi.glyph.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import io.github.mangi.glyph.ui.component.blur.LocalBlurEnabled
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.LocalContentColor
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeColorSpec
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.theme.platformDynamicColors

@Composable
fun GlyphAppTheme(
    themeConfig: ThemeConfig = ThemeConfig(),
    content: @Composable () -> Unit,
) {
    val colorSchemeMode = when {
        !themeConfig.useMonet && themeConfig.colorMode == 1 -> ColorSchemeMode.Light
        !themeConfig.useMonet && themeConfig.colorMode == 2 -> ColorSchemeMode.Dark
        !themeConfig.useMonet -> ColorSchemeMode.System
        themeConfig.colorMode == 1 -> ColorSchemeMode.MonetLight
        themeConfig.colorMode == 2 -> ColorSchemeMode.MonetDark
        else -> ColorSchemeMode.MonetSystem
    }
    val isDark = themeConfig.resolveIsDark(isSystemInDarkTheme())
    val systemSeedColor = if (themeConfig.useMonet && themeConfig.accentColor == ThemeAccentColor.Default) {
        platformDynamicColors(isDark).primary
    } else {
        null
    }
    val keyColor = when {
        !themeConfig.useMonet -> null
        themeConfig.accentColor == ThemeAccentColor.Default -> systemSeedColor
        else -> themeConfig.accentColor.seedColor
    }
    val controller = remember(themeConfig, colorSchemeMode, keyColor, isDark) {
        ThemeController(
            colorSchemeMode = colorSchemeMode,
            keyColor = keyColor,
            colorSpec = ThemeColorSpec.Spec2025,
            paletteStyle = themeConfig.paletteStyle,
        )
    }
    val colors = controller.currentColors()
    val themedColors = remember(colors, isDark, themeConfig.pureBlack) {
        if (themeConfig.useMonet && themeConfig.pureBlack && isDark) {
            colors.copy(
                background = Color.Black,
                surface = Color.Black,
            )
        } else {
            colors
        }
    }

    MiuixTheme(colors = themedColors) {
        val currentDensity = LocalDensity.current
        val appDensity = remember(currentDensity, themeConfig.densityScale) {
            Density(
                density = currentDensity.density * themeConfig.densityScale,
                fontScale = currentDensity.fontScale,
            )
        }
        CompositionLocalProvider(
            LocalAppDarkMode provides isDark,
            LocalAppMonetEnabled provides themeConfig.useMonet,
            LocalPlatformDensity provides currentDensity,
            LocalDensity provides appDensity,
            LocalBlurEnabled provides themeConfig.blurEnabled,
            LocalContentColor provides MiuixTheme.colorScheme.onBackground,
        ) {
            content()
        }
    }
}
