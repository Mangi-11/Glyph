package io.github.mangi.glyph.ui.theme

import android.content.Context
import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.theme.ThemePaletteStyle

data class ThemeConfig(
    val colorMode: Int = 0,
    val pureBlack: Boolean = false,
    val useMonet: Boolean = false,
    val paletteStyle: ThemePaletteStyle = ThemePaletteStyle.TonalSpot,
    val accentColor: ThemeAccentColor = ThemeAccentColor.Default,
    val blurEnabled: Boolean = true,
    val floatingBottomBar: Boolean = false,
    val floatingBottomBarStyle: FloatingBottomBarStyle = FloatingBottomBarStyle.Miuix,
    val bottomBarMode: BottomBarMode = BottomBarMode.IconAndText,
    val densityScale: Float = DefaultDensityScale,
    val predictiveBackToHomeEnabled: Boolean = true,
)

const val MinDensityScale = 0.8f
const val MaxDensityScale = 1.1f
const val DefaultDensityScale = 1f

fun ThemeConfig.resolveIsDark(systemDark: Boolean): Boolean = when (colorMode) {
    1 -> false
    2 -> true
    else -> systemDark
}

fun normalizeDensityScale(value: Float): Float =
    if (value.isFinite()) value.coerceIn(MinDensityScale, MaxDensityScale) else DefaultDensityScale

enum class FloatingBottomBarStyle(val storageValue: String) {
    Miuix("miuix"),
    IosLike("ios_like");

    companion object {
        fun fromStorage(value: String): FloatingBottomBarStyle =
            entries.firstOrNull { it.storageValue == value } ?: Miuix
    }
}

enum class BottomBarMode(val storageValue: String) {
    IconAndText("icon_and_text"),
    IconOnly("icon_only");

    companion object {
        fun fromStorage(value: String): BottomBarMode =
            entries.firstOrNull { it.storageValue == value } ?: IconAndText
    }
}

enum class ThemeAccentColor(
    val storageValue: String,
    val seedColor: Color,
) {
    Default("default", Color(0xFF3482FF)),
    Blue("blue", Color(0xFF3482FF)),
    Purple("purple", Color(0xFF6750A4)),
    Pink("pink", Color(0xFFB0006D)),
    Red("red", Color(0xFFBA1A1A)),
    Orange("orange", Color(0xFFB65D00)),
    Yellow("yellow", Color(0xFF7D5700)),
    Green("green", Color(0xFF006D3B)),
    Teal("teal", Color(0xFF006A6A));

    companion object {
        fun fromStorage(value: String): ThemeAccentColor =
            entries.firstOrNull { it.storageValue == value } ?: Default
    }
}

val ThemePaletteStyles: List<ThemePaletteStyle> = ThemePaletteStyle.entries.toList()

fun themePaletteStyleFromStorage(value: String): ThemePaletteStyle =
    ThemePaletteStyles.firstOrNull { it.name == value } ?: ThemePaletteStyle.TonalSpot

private object ThemePreferenceKeys {
    const val PREFS = "glyph_theme_prefs"
    const val DARK_MODE = "dark_mode"
    const val THEME_PURE_BLACK = "theme_pure_black"
    const val THEME_MONET = "theme_monet"
    const val THEME_PALETTE_STYLE = "theme_palette_style"
    const val THEME_ACCENT_COLOR = "theme_accent_color"
    const val THEME_BLUR = "theme_blur"
    const val THEME_FLOATING_BOTTOM_BAR = "theme_floating_bottom_bar"
    const val THEME_FLOATING_BOTTOM_BAR_STYLE = "theme_floating_bottom_bar_style"
    const val THEME_BOTTOM_BAR_MODE = "theme_bottom_bar_mode"
    const val THEME_DENSITY_SCALE = "theme_density_scale"
    const val PREDICTIVE_BACK = "predictive_back"
}

fun readThemeConfig(context: Context): ThemeConfig {
    val prefs = context.getSharedPreferences(ThemePreferenceKeys.PREFS, Context.MODE_PRIVATE)
    val colorMode = when (prefs.getString(ThemePreferenceKeys.DARK_MODE, "system")) {
        "light" -> 1
        "dark" -> 2
        else -> 0
    }
    return ThemeConfig(
        colorMode = colorMode,
        pureBlack = prefs.getString(ThemePreferenceKeys.THEME_PURE_BLACK, "false") == "true",
        useMonet = prefs.getString(ThemePreferenceKeys.THEME_MONET, "false") == "true",
        paletteStyle = themePaletteStyleFromStorage(
            prefs.getString(ThemePreferenceKeys.THEME_PALETTE_STYLE, ThemePaletteStyle.TonalSpot.name)
                ?: ThemePaletteStyle.TonalSpot.name,
        ),
        accentColor = ThemeAccentColor.fromStorage(
            prefs.getString(ThemePreferenceKeys.THEME_ACCENT_COLOR, ThemeAccentColor.Default.storageValue)
                ?: ThemeAccentColor.Default.storageValue,
        ),
        blurEnabled = prefs.getString(ThemePreferenceKeys.THEME_BLUR, "true") != "false",
        floatingBottomBar = prefs.getString(ThemePreferenceKeys.THEME_FLOATING_BOTTOM_BAR, "false") == "true",
        floatingBottomBarStyle = FloatingBottomBarStyle.fromStorage(
            prefs.getString(
                ThemePreferenceKeys.THEME_FLOATING_BOTTOM_BAR_STYLE,
                FloatingBottomBarStyle.Miuix.storageValue,
            ) ?: FloatingBottomBarStyle.Miuix.storageValue,
        ),
        bottomBarMode = BottomBarMode.fromStorage(
            prefs.getString(ThemePreferenceKeys.THEME_BOTTOM_BAR_MODE, BottomBarMode.IconAndText.storageValue)
                ?: BottomBarMode.IconAndText.storageValue,
        ),
        densityScale = normalizeDensityScale(
            prefs.getString(ThemePreferenceKeys.THEME_DENSITY_SCALE, DefaultDensityScale.toString())
                ?.toFloatOrNull()
                ?: DefaultDensityScale,
        ),
        predictiveBackToHomeEnabled = prefs.getString(ThemePreferenceKeys.PREDICTIVE_BACK, "true") != "false",
    )
}

fun writeThemeConfig(context: Context, config: ThemeConfig) {
    val prefs = context.getSharedPreferences(ThemePreferenceKeys.PREFS, Context.MODE_PRIVATE)
    prefs.edit()
        .putString(
            ThemePreferenceKeys.DARK_MODE,
            when (config.colorMode) {
                1 -> "light"
                2 -> "dark"
                else -> "system"
            },
        )
        .putString(ThemePreferenceKeys.THEME_PURE_BLACK, config.pureBlack.toString())
        .putString(ThemePreferenceKeys.THEME_MONET, config.useMonet.toString())
        .putString(ThemePreferenceKeys.THEME_PALETTE_STYLE, config.paletteStyle.name)
        .putString(ThemePreferenceKeys.THEME_ACCENT_COLOR, config.accentColor.storageValue)
        .putString(ThemePreferenceKeys.THEME_BLUR, config.blurEnabled.toString())
        .putString(ThemePreferenceKeys.THEME_FLOATING_BOTTOM_BAR, config.floatingBottomBar.toString())
        .putString(ThemePreferenceKeys.THEME_FLOATING_BOTTOM_BAR_STYLE, config.floatingBottomBarStyle.storageValue)
        .putString(ThemePreferenceKeys.THEME_BOTTOM_BAR_MODE, config.bottomBarMode.storageValue)
        .putString(ThemePreferenceKeys.THEME_DENSITY_SCALE, normalizeDensityScale(config.densityScale).toString())
        .putString(ThemePreferenceKeys.PREDICTIVE_BACK, config.predictiveBackToHomeEnabled.toString())
        .apply()
}
