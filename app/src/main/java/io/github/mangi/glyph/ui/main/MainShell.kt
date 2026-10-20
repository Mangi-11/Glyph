package io.github.mangi.glyph.ui.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import io.github.mangi.glyph.R
import io.github.mangi.glyph.ui.component.blur.BlurredBar
import io.github.mangi.glyph.ui.component.blur.rememberBlurBackdrop
import io.github.mangi.glyph.ui.liquid.IosLiquidGlassNavigationBar
import io.github.mangi.glyph.ui.theme.BottomBarMode
import io.github.mangi.glyph.ui.theme.FloatingBottomBarStyle
import io.github.mangi.glyph.ui.theme.LocalAppDarkMode
import io.github.mangi.glyph.ui.theme.ThemeConfig
import top.yukonga.miuix.kmp.basic.FloatingNavigationBar
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarDisplayMode
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.NavigationItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SnackbarHost
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.highlight.Highlight
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Home
import top.yukonga.miuix.kmp.icon.extended.ListView
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.theme.MiuixTheme

enum class MainTab(
    val index: Int,
    val labelRes: Int,
    val icon: ImageVector,
) {
    Home(0, R.string.tab_home, MiuixIcons.Home),
    Rules(1, R.string.tab_rules, MiuixIcons.ListView),
    Settings(2, R.string.tab_settings, MiuixIcons.Settings),
    ;

    companion object {
        val all = arrayOf(Home, Rules, Settings)
    }
}

class MainTabSlot(
    val content: @Composable (bottomPadding: Dp) -> Unit,
)

@Composable
fun MainShell(
    home: MainTabSlot,
    rules: MainTabSlot,
    about: MainTabSlot,
    snackbarHostState: SnackbarHostState,
    rulesIsRootNavigation: Boolean,
    settingsIsRootNavigation: Boolean,
    themeConfig: ThemeConfig,
) {
    val tabs = MainTab.all
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val mainPagerState = rememberMainPagerState(pagerState)
    val slots = arrayOf(home, rules, about)
    val bottomBarBackdrop = rememberBlurBackdrop()
    val navigationItems = tabs.map { tab ->
        NavigationItem(label = stringResource(tab.labelRes), icon = tab.icon)
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect {
            mainPagerState.syncPage()
        }
    }

    val bottomBarBlurActive = bottomBarBackdrop != null
    val barColor = if (bottomBarBlurActive) Color.Transparent else MiuixTheme.colorScheme.surface
    val floatingBarColor = if (bottomBarBlurActive) Color.Transparent else MiuixTheme.colorScheme.surfaceContainer
    val floatingPillRadius = 50.dp
    val floatingBarShape = RoundedCornerShape(floatingPillRadius)
    val isDark = LocalAppDarkMode.current
    val floatingHighlight = remember(isDark) {
        if (isDark) Highlight.GlassStrokeMiddleDark else Highlight.GlassStrokeMiddleLight
    }
    val floatingBarModifier = if (bottomBarBackdrop != null) {
        Modifier.textureBlur(
            backdrop = bottomBarBackdrop,
            shape = floatingBarShape,
            blurRadius = 25f,
            colors = BlurDefaults.blurColors(
                blendColors = listOf(
                    BlendColorEntry(
                        color = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.6f),
                    ),
                ),
            ),
            highlight = floatingHighlight,
        )
    } else {
        Modifier
    }
    val bottomBarDisplayMode = when (themeConfig.bottomBarMode) {
        BottomBarMode.IconAndText -> NavigationBarDisplayMode.IconAndText
        BottomBarMode.IconOnly -> NavigationBarDisplayMode.IconOnly
    }
    val showBottomBarLabels = themeConfig.bottomBarMode == BottomBarMode.IconAndText
    val hideBottomBar by remember(mainPagerState, rulesIsRootNavigation, settingsIsRootNavigation) {
        derivedStateOf {
            when (mainPagerState.selectedPage) {
                MainTab.Rules.index -> !rulesIsRootNavigation
                MainTab.Settings.index -> !settingsIsRootNavigation
                else -> false
            }
        }
    }
    val currentTabIsRootNavigation by remember(mainPagerState, rulesIsRootNavigation, settingsIsRootNavigation) {
        derivedStateOf {
            when (mainPagerState.selectedPage) {
                MainTab.Rules.index -> rulesIsRootNavigation
                MainTab.Settings.index -> settingsIsRootNavigation
                else -> true
            }
        }
    }

    MainScreenBackHandler(
        mainPagerState = mainPagerState,
        isRootNavigation = currentTabIsRootNavigation,
        predictiveBackToHomeEnabled = themeConfig.predictiveBackToHomeEnabled,
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (!hideBottomBar) {
            if (themeConfig.floatingBottomBar) {
                if (themeConfig.floatingBottomBarStyle == FloatingBottomBarStyle.IosLike) {
                    IosLiquidGlassNavigationBar(
                        items = navigationItems,
                        selectedIndex = mainPagerState.selectedPage.coerceIn(0, tabs.lastIndex),
                        onItemClick = mainPagerState::animateToPage,
                        backdrop = bottomBarBackdrop,
                        isBlurActive = bottomBarBlurActive,
                        showLabels = showBottomBarLabels,
                    )
                } else {
                    FloatingNavigationBar(
                        modifier = floatingBarModifier,
                        color = floatingBarColor,
                        cornerRadius = floatingPillRadius,
                    ) {
                        navigationItems.forEachIndexed { index, item ->
                            MiuixFloatingNavigationBarItem(
                                item = item,
                                selected = mainPagerState.selectedPage == index,
                                onClick = { mainPagerState.animateToPage(index) },
                                showLabel = showBottomBarLabels,
                            )
                        }
                    }
                }
            } else {
                BlurredBar(backdrop = bottomBarBackdrop, blurActive = bottomBarBlurActive) {
                    NavigationBar(
                        color = barColor,
                        mode = bottomBarDisplayMode,
                    ) {
                        navigationItems.forEachIndexed { index, item ->
                            NavigationBarItem(
                                selected = mainPagerState.selectedPage == index,
                                onClick = { mainPagerState.animateToPage(index) },
                                icon = item.icon,
                                label = item.label,
                            )
                        }
                    }
                }
            }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        HorizontalPager(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (bottomBarBackdrop != null) {
                        Modifier.layerBackdrop(bottomBarBackdrop)
                    } else {
                        Modifier
                    },
                ),
            state = pagerState,
            userScrollEnabled = currentTabIsRootNavigation,
            beyondViewportPageCount = 2,
        ) { index ->
            slots[index].content(padding.calculateBottomPadding())
        }
    }
}

@Composable
private fun MiuixFloatingNavigationBarItem(
    item: NavigationItem,
    selected: Boolean,
    onClick: () -> Unit,
    showLabel: Boolean,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val onSurfaceContainerColor = MiuixTheme.colorScheme.onSurfaceContainer
    val tint = when {
        isPressed -> onSurfaceContainerColor.copy(alpha = if (selected) 0.7f else 0.5f)
        selected -> onSurfaceContainerColor
        else -> onSurfaceContainerColor.copy(alpha = 0.6f)
    }

    Column(
        modifier = modifier
            .defaultMinSize(minWidth = if (showLabel) 56.dp else 48.dp, minHeight = 48.dp)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Tab,
                interactionSource = interactionSource,
                indication = null,
            )
            .padding(horizontal = if (showLabel) 8.dp else 6.dp, vertical = 5.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            modifier = Modifier.size(22.dp),
            imageVector = item.icon,
            contentDescription = if (showLabel) null else item.label,
            tint = tint,
        )
        if (showLabel) {
            Text(
                text = item.label,
                color = tint,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun MainScreenBackHandler(
    mainPagerState: MainPagerState,
    isRootNavigation: Boolean,
    predictiveBackToHomeEnabled: Boolean,
) {
    val enabled by remember(mainPagerState, isRootNavigation, predictiveBackToHomeEnabled) {
        derivedStateOf {
            predictiveBackToHomeEnabled &&
                isRootNavigation &&
                mainPagerState.selectedPage != MainTab.Home.index
        }
    }
    val navigationEventState = rememberNavigationEventState(NavigationEventInfo.None)

    NavigationBackHandler(
        state = navigationEventState,
        isBackEnabled = enabled,
        onBackCompleted = { mainPagerState.animateToPage(MainTab.Home.index) },
    )
}
