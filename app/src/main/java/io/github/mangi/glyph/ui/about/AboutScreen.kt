package io.github.mangi.glyph.ui.about

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.mangi.glyph.BuildConfig
import io.github.mangi.glyph.R
import io.github.mangi.glyph.rules.RuleStore
import io.github.mangi.glyph.ui.component.AdaptiveTopAppBar
import io.github.mangi.glyph.ui.component.CardItem
import io.github.mangi.glyph.ui.component.blur.BlurredBar
import io.github.mangi.glyph.ui.component.blur.rememberBlurBackdrop
import io.github.mangi.glyph.ui.component.groupedCardItems
import io.github.mangi.glyph.ui.home.HomeScreenState
import io.github.mangi.glyph.ui.settings.ModuleSettingsSection
import io.github.mangi.glyph.ui.theme.ThemeConfig
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.nav.core.NavDisplay
import top.yukonga.miuix.kmp.nav.core.NavDisplayEffects
import top.yukonga.miuix.kmp.nav.core.NavKey
import top.yukonga.miuix.kmp.nav.core.navBackStackOf
import top.yukonga.miuix.kmp.nav.core.rememberNavSystemCornerRadius
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

private sealed interface SettingsRoute : NavKey {
    data object Main : SettingsRoute
    data object ThemeSettings : SettingsRoute
    data object AboutDetails : SettingsRoute
}

@Composable
fun AboutScreen(
    bottomPadding: Dp,
    homeState: HomeScreenState,
    themeConfig: ThemeConfig,
    onThemeConfigChange: (ThemeConfig) -> Unit,
    onPredictiveBackChange: (Boolean) -> Unit,
    onRulesEnabledChange: (Boolean) -> Unit,
    onIconSourceModeChange: (RuleStore.IconSourceMode) -> Unit,
    onPanelIconReplacementEnabledChange: (Boolean) -> Unit,
    onLockScreenCapsuleIconReplacementEnabledChange: (Boolean) -> Unit,
    onOplusPushSpecialHandlingEnabledChange: (Boolean) -> Unit,
    onPlaceholderIconEnabledChange: (Boolean) -> Unit,
    onLauncherIconHiddenChange: (Boolean) -> Unit,
    onRootNavigationChange: (Boolean) -> Unit = {},
) {
    val context = LocalContext.current
    val backStack = remember { navBackStackOf(SettingsRoute.Main) }
    val isRootNavigation by remember {
        derivedStateOf { backStack.size <= 1 }
    }
    LaunchedEffect(isRootNavigation) {
        onRootNavigationChange(isRootNavigation)
    }
    NavDisplay(
        backStack = backStack,
        modifier = Modifier.fillMaxSize(),
        onBack = { backStack.removeLastOrNull() },
        effects = NavDisplayEffects(cornerClipRadius = rememberNavSystemCornerRadius()),
    ) {
        entry<SettingsRoute.Main> {
            SettingsMainScreen(
                bottomPadding = bottomPadding,
                homeState = homeState,
                onRulesEnabledChange = onRulesEnabledChange,
                onIconSourceModeChange = onIconSourceModeChange,
                onPanelIconReplacementEnabledChange = onPanelIconReplacementEnabledChange,
                onLockScreenCapsuleIconReplacementEnabledChange =
                    onLockScreenCapsuleIconReplacementEnabledChange,
                onOplusPushSpecialHandlingEnabledChange = onOplusPushSpecialHandlingEnabledChange,
                onPlaceholderIconEnabledChange = onPlaceholderIconEnabledChange,
                onLauncherIconHiddenChange = onLauncherIconHiddenChange,
                onOpenThemeSettings = { backStack.add(SettingsRoute.ThemeSettings) },
                onOpenAboutDetails = { backStack.add(SettingsRoute.AboutDetails) },
            )
        }
        entry<SettingsRoute.ThemeSettings> {
            ThemeSettingsScreen(
                context = context,
                themeConfig = themeConfig,
                onThemeConfigChange = onThemeConfigChange,
                onPredictiveBackChange = onPredictiveBackChange,
                onBack = { backStack.removeLastOrNull() },
            )
        }
        entry<SettingsRoute.AboutDetails> {
            AboutDetailsScreen(
                onBack = { backStack.removeLastOrNull() },
                onOpenUrl = { url -> context.openUrl(url) },
            )
        }
    }
}

@Composable
private fun SettingsMainScreen(
    bottomPadding: Dp,
    homeState: HomeScreenState,
    onRulesEnabledChange: (Boolean) -> Unit,
    onIconSourceModeChange: (RuleStore.IconSourceMode) -> Unit,
    onPanelIconReplacementEnabledChange: (Boolean) -> Unit,
    onLockScreenCapsuleIconReplacementEnabledChange: (Boolean) -> Unit,
    onOplusPushSpecialHandlingEnabledChange: (Boolean) -> Unit,
    onPlaceholderIconEnabledChange: (Boolean) -> Unit,
    onLauncherIconHiddenChange: (Boolean) -> Unit,
    onOpenThemeSettings: () -> Unit,
    onOpenAboutDetails: () -> Unit,
) {
    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberBlurBackdrop()
    val blurActive = backdrop != null
    val barColor = if (blurActive) Color.Transparent else MiuixTheme.colorScheme.surface

    Scaffold(
        topBar = {
            BlurredBar(backdrop = backdrop, blurActive = blurActive) {
                AdaptiveTopAppBar(
                    title = stringResource(R.string.tab_settings),
                    color = barColor,
                    scrollBehavior = scrollBehavior,
                )
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier)
                .scrollEndHaptic()
                .overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
                bottom = maxOf(bottomPadding, innerPadding.calculateBottomPadding()),
            ),
        ) {
            groupedCardItems(
                keyPrefix = "settings_module",
                outerTopPadding = 12.dp,
                outerBottomPadding = 12.dp,
                items = listOf(
                    CardItem("moduleSettings") {
                        ModuleSettingsSection(
                            state = homeState,
                            onIconSourceModeChange = onIconSourceModeChange,
                            onRulesEnabledChange = onRulesEnabledChange,
                            onPanelIconReplacementEnabledChange = onPanelIconReplacementEnabledChange,
                            onLockScreenCapsuleIconReplacementEnabledChange =
                                onLockScreenCapsuleIconReplacementEnabledChange,
                            onOplusPushSpecialHandlingEnabledChange = onOplusPushSpecialHandlingEnabledChange,
                            onPlaceholderIconEnabledChange = onPlaceholderIconEnabledChange,
                        )
                    },
                ),
            )
            groupedCardItems(
                keyPrefix = "settings_app",
                outerBottomPadding = 12.dp,
                items = listOf(
                    CardItem("hideLauncherIcon") {
                        SwitchPreference(
                            title = stringResource(R.string.label_hide_launcher_icon),
                            checked = homeState.launcherIconHidden,
                            onCheckedChange = onLauncherIconHiddenChange,
                        )
                    },
                ),
            )
            groupedCardItems(
                keyPrefix = "settings_appearance",
                outerBottomPadding = 12.dp,
                items = listOf(
                    CardItem("theme") {
                        ArrowPreference(
                            title = stringResource(R.string.settings_theme_title),
                            summary = stringResource(R.string.settings_theme_summary),
                            onClick = onOpenThemeSettings,
                        )
                    },
                ),
            )
            groupedCardItems(
                keyPrefix = "settings_about",
                outerBottomPadding = 24.dp,
                items = listOf(
                    CardItem("about") {
                        ArrowPreference(
                            title = stringResource(R.string.about_title),
                            summary = "Glyph v${BuildConfig.VERSION_NAME}",
                            onClick = onOpenAboutDetails,
                        )
                    },
                ),
            )
        }
    }
}

private fun Context.openUrl(url: String) {
    runCatching {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
