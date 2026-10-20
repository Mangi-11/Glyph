@file:JvmName("label")

package io.github.mangi.glyph.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.mangi.glyph.R
import top.yukonga.miuix.kmp.theme.ThemePaletteStyle

val ThemeColorModes = listOf(0, 1, 2)

@Composable
fun themeColorModeLabel(colorMode: Int): String = stringResource(
    when (colorMode) {
        1 -> R.string.settings_theme_light
        2 -> R.string.settings_theme_dark
        else -> R.string.settings_theme_system
    },
)

@Composable
fun ThemeAccentColor.label(): String = stringResource(
    when (this) {
        ThemeAccentColor.Default -> R.string.settings_theme_accent_default
        ThemeAccentColor.Blue -> R.string.settings_theme_accent_blue
        ThemeAccentColor.Purple -> R.string.settings_theme_accent_purple
        ThemeAccentColor.Pink -> R.string.settings_theme_accent_pink
        ThemeAccentColor.Red -> R.string.settings_theme_accent_red
        ThemeAccentColor.Orange -> R.string.settings_theme_accent_orange
        ThemeAccentColor.Yellow -> R.string.settings_theme_accent_yellow
        ThemeAccentColor.Green -> R.string.settings_theme_accent_green
        ThemeAccentColor.Teal -> R.string.settings_theme_accent_teal
    },
)

@Composable
fun ThemePaletteStyle.label(): String = stringResource(
    when (this) {
        ThemePaletteStyle.Neutral -> R.string.settings_theme_palette_neutral
        ThemePaletteStyle.Vibrant -> R.string.settings_theme_palette_vibrant
        ThemePaletteStyle.Expressive -> R.string.settings_theme_palette_expressive
        ThemePaletteStyle.Rainbow -> R.string.settings_theme_palette_rainbow
        ThemePaletteStyle.FruitSalad -> R.string.settings_theme_palette_fruit_salad
        ThemePaletteStyle.Monochrome -> R.string.settings_theme_palette_monochrome
        ThemePaletteStyle.Fidelity -> R.string.settings_theme_palette_fidelity
        ThemePaletteStyle.Content -> R.string.settings_theme_palette_content
        else -> R.string.settings_theme_palette_tonal_spot
    },
)

@Composable
fun FloatingBottomBarStyle.label(): String = stringResource(
    when (this) {
        FloatingBottomBarStyle.Miuix -> R.string.settings_theme_floating_bottom_bar_style_miuix
        FloatingBottomBarStyle.IosLike -> R.string.settings_theme_floating_bottom_bar_style_ios_like
    },
)

@Composable
fun BottomBarMode.label(): String = stringResource(
    when (this) {
        BottomBarMode.IconAndText -> R.string.settings_theme_bottom_bar_icon_and_text
        BottomBarMode.IconOnly -> R.string.settings_theme_bottom_bar_icon_only
    },
)
