package io.github.mangi.glyph.ui.rules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.mangi.glyph.R
import io.github.mangi.glyph.rules.IconRule
import io.github.mangi.glyph.rules.RuleStore
import io.github.mangi.glyph.ui.component.MiuixBlurredTopBar
import io.github.mangi.glyph.ui.component.rememberMiuixPageBackdrop
import io.github.mangi.glyph.ui.theme.GlyphTheme
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SearchBar
import top.yukonga.miuix.kmp.basic.SearchBarDefaults
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.SnackbarHost
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ChevronBackward
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

private sealed interface RuleListRoute : NavKey {
    data object List : RuleListRoute
    data class IconPicker(val rule: IconRule) : RuleListRoute
    data object UnadaptedAppPicker : RuleListRoute
    data class UnadaptedIconPicker(val app: InstalledAppChoice) : RuleListRoute
}

@Composable
internal fun RuleNavigationHost(
    state: RuleListState,
    onRuleIconSourceChange: (IconRule, String?, (String) -> Unit) -> Unit,
    onBindUnadaptedApp: (InstalledAppChoice, String, (String) -> Unit) -> Unit,
    snackbarHostState: SnackbarHostState,
    onRootNavigationChange: (Boolean) -> Unit = {},
    content: @Composable (
        onChooseIcon: (IconRule) -> Unit,
        onAddUnadaptedApp: () -> Unit,
    ) -> Unit,
) {
    val backStack = remember { navBackStackOf(RuleListRoute.List) }
    val scope = rememberCoroutineScope()
    val isRootNavigation by remember {
        derivedStateOf { backStack.size <= 1 }
    }
    LaunchedEffect(isRootNavigation) {
        onRootNavigationChange(isRootNavigation)
    }

    fun showSnackbar(message: String) {
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    fun returnToList() {
        backStack.subList(1, backStack.size).clear()
    }

    fun navigate(route: RuleListRoute) {
        if (route !in backStack) backStack.add(route)
    }

    NavDisplay(
        backStack = backStack,
        modifier = Modifier.fillMaxSize(),
        onBack = { backStack.removeLastOrNull() },
        effects = NavDisplayEffects(
            cornerClipRadius = rememberNavSystemCornerRadius(),
        ),
    ) {
        entry<RuleListRoute.List> {
            content(
                { navigate(RuleListRoute.IconPicker(it)) },
                { navigate(RuleListRoute.UnadaptedAppPicker) },
            )
        }
        entry<RuleListRoute.IconPicker> { route ->
            IconPickerScreen(
                targetAppName = route.rule.appName,
                targetPackageName = route.rule.packageName,
                currentSourcePackage = route.rule.iconSourcePackage,
                libraryRules = state.libraryRules,
                canClear = route.rule.hasManualIcon,
                onSelect = { source ->
                    returnToList()
                    onRuleIconSourceChange(route.rule, source.packageName, ::showSnackbar)
                },
                onClear = {
                    returnToList()
                    onRuleIconSourceChange(route.rule, null, ::showSnackbar)
                },
                onBack = { backStack.removeLastOrNull() },
            )
        }
        entry<RuleListRoute.UnadaptedAppPicker> {
            InstalledAppPickerScreen(
                apps = state.unadaptedInstalledApps,
                onSelect = { navigate(RuleListRoute.UnadaptedIconPicker(it)) },
                onBack = { backStack.removeLastOrNull() },
            )
        }
        entry<RuleListRoute.UnadaptedIconPicker> { route ->
            IconPickerScreen(
                targetAppName = route.app.label,
                targetPackageName = route.app.packageName,
                currentSourcePackage = null,
                libraryRules = state.libraryRules,
                canClear = false,
                onSelect = { source ->
                    returnToList()
                    onBindUnadaptedApp(route.app, source.packageName, ::showSnackbar)
                },
                onClear = { backStack.removeLastOrNull() },
                onBack = { backStack.removeLastOrNull() },
            )
        }
    }
}

@Composable
fun RuleListScreen(
    state: RuleListState,
    onQueryChange: (String) -> Unit,
    onRuleEnabledChange: (IconRule, Boolean, (String) -> Unit) -> Unit,
    onRuleEnabledAllChange: (IconRule, Boolean, (String) -> Unit) -> Unit,
    onInstalledRulesEnabledAllChange: (Boolean, (String) -> Unit) -> Unit,
    bottomPadding: Dp,
    snackbarHostState: SnackbarHostState,
    onChooseIcon: (IconRule) -> Unit,
    onAddUnadaptedApp: () -> Unit,
    navigationIcon: @Composable () -> Unit = {},
    showSnackbarHost: Boolean = false,
) {
    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberMiuixPageBackdrop()
    val surfaceColor = MiuixTheme.colorScheme.surface
    val scope = rememberCoroutineScope()

    fun showSnackbar(message: String) {
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    Scaffold(
        topBar = {
            MiuixBlurredTopBar(backdrop) {
                TopAppBar(
                    title = stringResource(R.string.rules_title),
                    color = if (backdrop != null) Color.Transparent else surfaceColor,
                    navigationIcon = navigationIcon,
                    scrollBehavior = scrollBehavior,
                )
            }
        },
        snackbarHost = {
            if (showSnackbarHost) SnackbarHost(snackbarHostState)
        },
    ) { innerPadding ->
        RuleListContent(
            state = state,
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
                bottom = maxOf(bottomPadding, innerPadding.calculateBottomPadding()),
            ),
            scrollBehavior = scrollBehavior,
            backdrop = backdrop,
            onQueryChange = onQueryChange,
            onRuleEnabledChange = onRuleEnabledChange,
            onRuleEnabledAllChange = onRuleEnabledAllChange,
            onInstalledRulesEnabledAllChange = onInstalledRulesEnabledAllChange,
            onShowMessage = ::showSnackbar,
            onChooseIcon = onChooseIcon,
            onAddUnadaptedApp = onAddUnadaptedApp,
        )
    }
}

@Composable
private fun RuleListContent(
    state: RuleListState,
    contentPadding: PaddingValues,
    scrollBehavior: top.yukonga.miuix.kmp.basic.ScrollBehavior,
    backdrop: LayerBackdrop?,
    onQueryChange: (String) -> Unit,
    onRuleEnabledChange: (IconRule, Boolean, (String) -> Unit) -> Unit,
    onRuleEnabledAllChange: (IconRule, Boolean, (String) -> Unit) -> Unit,
    onInstalledRulesEnabledAllChange: (Boolean, (String) -> Unit) -> Unit,
    onShowMessage: (String) -> Unit,
    onChooseIcon: (IconRule) -> Unit,
    onAddUnadaptedApp: () -> Unit,
) {
    var searchExpanded by remember { mutableStateOf(false) }
    val ruleLibraryMode = state.config.iconSourceMode == RuleStore.IconSourceMode.RuleLibrary
    val canChooseIcon = state.canEditConfig && state.config.rulesEnabled && ruleLibraryMode

    val sections = state.sections
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (backdrop != null) {
                    Modifier.layerBackdrop(backdrop)
                } else {
                    Modifier
                },
            )
            .scrollEndHaptic()
            .overScrollVertical()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        contentPadding = contentPadding,
    ) {
            item {
                SearchBar(
                    modifier = Modifier.padding(bottom = SearchBarDefaults.InsideMargin.width),
                    inputField = {
                        InputField(
                            query = state.query,
                            onQueryChange = onQueryChange,
                            onSearch = { searchExpanded = false },
                            expanded = searchExpanded,
                            onExpandedChange = { searchExpanded = it },
                            label = stringResource(R.string.rules_search_hint),
                        )
                    },
                    onExpandedChange = { searchExpanded = it },
                    expanded = searchExpanded,
                    outsideEndAction = {
                        TextButton(
                            text = stringResource(R.string.dialog_cancel),
                            modifier = Modifier.padding(end = SearchBarDefaults.InsideMargin.width),
                            onClick = {
                                onQueryChange("")
                                searchExpanded = false
                            },
                        )
                    },
                ) {}
            }
            if (sections.isEmpty()) {
                item { SmallTitle(text = stringResource(R.string.section_rule_management)) }
                item {
                    EmptyRulesCard(
                        query = state.query,
                        isLoading = state.isLoading,
                        loadFailed = state.loadFailed,
                    )
                }
            } else {
                if (state.libraryRules.isNotEmpty() && state.query.isBlank()) {
                    item(key = "add:unadapted") {
                        AddUnadaptedAppCard(
                            enabled = canChooseIcon,
                            onClick = onAddUnadaptedApp,
                        )
                    }
                }
                sections.forEach { section ->
                    item(key = "section:${section.type}") {
                        RuleSectionTitle(section)
                    }
                    if (section.type == RuleSectionType.Installed && state.query.isBlank()) {
                        item(key = "installed:enabled_all") {
                            InstalledRulesEnabledAllCard(
                                checked = state.installedRulesEnabledAll,
                                enabled = state.canEditConfig &&
                                    state.config.rulesEnabled &&
                                    state.config.iconSourceMode == RuleStore.IconSourceMode.RuleLibrary &&
                                    state.installedEnabledRulePackageNames.isNotEmpty(),
                                onCheckedChange = {
                                    onInstalledRulesEnabledAllChange(it, onShowMessage)
                                },
                            )
                        }
                    }
                    items(
                        items = section.rules,
                        key = { it.packageName },
                    ) { rule ->
                        RuleCard(
                            rule = rule,
                            rulesEnabled = state.config.rulesEnabled,
                            ruleLibraryMode = ruleLibraryMode,
                            canEditConfig = state.canEditConfig,
                            onEnabledChange = { onRuleEnabledChange(rule, it, onShowMessage) },
                            onEnabledAllChange = { onRuleEnabledAllChange(rule, it, onShowMessage) },
                            onChooseIcon = { onChooseIcon(rule) },
                        )
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
}

@Composable
private fun AddUnadaptedAppCard(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .padding(bottom = 10.dp),
        insideMargin = PaddingValues(0.dp),
    ) {
        ArrowPreference(
            title = stringResource(R.string.label_add_unadapted_app),
            summary = stringResource(R.string.label_add_unadapted_app_summary),
            onClick = onClick,
            enabled = enabled,
        )
    }
}

@Composable
private fun InstalledRulesEnabledAllCard(
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Card(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .padding(bottom = 10.dp),
        insideMargin = PaddingValues(0.dp),
    ) {
        SwitchPreference(
            title = stringResource(R.string.label_installed_rules_enabled_all),
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
        )
    }
}

@Composable
private fun RuleSectionTitle(section: RuleSection) {
    val text = when (section.type) {
        RuleSectionType.All -> stringResource(R.string.section_rule_management)
        RuleSectionType.Installed -> stringResource(R.string.rules_group_installed, section.rules.size)
        RuleSectionType.NotInstalled -> stringResource(R.string.rules_group_not_installed, section.rules.size)
    }
    SmallTitle(text = text)
}

@Composable
private fun RuleCard(
    rule: IconRule,
    rulesEnabled: Boolean,
    ruleLibraryMode: Boolean,
    canEditConfig: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onEnabledAllChange: (Boolean) -> Unit,
    onChooseIcon: () -> Unit,
) {
    val canChooseIcon = canEditConfig && rulesEnabled && ruleLibraryMode
    Card(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .padding(bottom = 10.dp),
        insideMargin = PaddingValues(0.dp),
    ) {
        BasicComponent(
            startAction = { RuleIcon(rule) },
            insideMargin = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            onClick = if (canChooseIcon) onChooseIcon else null,
        ) {
            Text(
                text = rule.appName.ifBlank { rule.packageName },
                style = MiuixTheme.textStyles.headline1,
                color = MiuixTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = buildString {
                    append(rule.packageName)
                    if (rule.contributorName.isNotBlank()) {
                        append(" · ")
                        append(rule.contributorName)
                    }
                },
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        ArrowPreference(
            title = stringResource(R.string.label_rule_choose_icon),
            summary = iconSourceSummary(rule),
            onClick = onChooseIcon,
            enabled = canChooseIcon,
        )
        ToggleComponent(
            title = stringResource(R.string.label_rule_enable),
            checked = rule.isEnabled,
            enabled = canChooseIcon,
            onCheckedChange = onEnabledChange,
        )
        ToggleComponent(
            title = stringResource(R.string.label_rule_force_all),
            checked = rule.isEnabledAll,
            enabled = canChooseIcon && rule.isEnabled,
            onCheckedChange = onEnabledAllChange,
        )
    }
}

@Composable
private fun iconSourceSummary(rule: IconRule): String = when {
    rule.iconSourcePackage == null -> stringResource(R.string.label_rule_choose_icon_default)
    rule.sourcedFrom != null -> stringResource(
        R.string.label_rule_choose_icon_using,
        rule.sourcedFrom.appName.ifBlank { rule.sourcedFrom.packageName },
    )
    else -> stringResource(R.string.label_rule_choose_icon_missing)
}

@Composable
private fun RuleIcon(rule: IconRule) {
    CatalogIcon(
        asset = rule.iconAsset,
        iconColor = rule.iconColor,
        packageName = rule.packageName,
    )
}

@Composable
private fun ToggleComponent(
    title: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    SwitchPreference(
        title = title,
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
    )
}

@Composable
private fun EmptyRulesCard(
    query: String,
    isLoading: Boolean,
    loadFailed: Boolean,
) {
    Card(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = when {
                    isLoading -> stringResource(R.string.rules_loading)
                    loadFailed -> stringResource(R.string.rules_load_failed)
                    query.isBlank() -> stringResource(R.string.rules_empty)
                    else -> stringResource(R.string.rules_search_empty)
                },
                style = MiuixTheme.textStyles.body1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
    }
}

@Composable
internal fun StandaloneRuleListScreen(
    state: RuleListState,
    onBack: () -> Unit,
    onQueryChange: (String) -> Unit,
    onRuleEnabledChange: (IconRule, Boolean, (String) -> Unit) -> Unit,
    onRuleEnabledAllChange: (IconRule, Boolean, (String) -> Unit) -> Unit,
    onInstalledRulesEnabledAllChange: (Boolean, (String) -> Unit) -> Unit,
    onRuleIconSourceChange: (IconRule, String?, (String) -> Unit) -> Unit,
    onBindUnadaptedApp: (InstalledAppChoice, String, (String) -> Unit) -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    RuleNavigationHost(
        state = state,
        onRuleIconSourceChange = onRuleIconSourceChange,
        onBindUnadaptedApp = onBindUnadaptedApp,
        snackbarHostState = snackbarHostState,
    ) { onChooseIcon, onAddUnadaptedApp ->
        RuleListScreen(
            state = state,
            onQueryChange = onQueryChange,
            onRuleEnabledChange = onRuleEnabledChange,
            onRuleEnabledAllChange = onRuleEnabledAllChange,
            onInstalledRulesEnabledAllChange = onInstalledRulesEnabledAllChange,
            bottomPadding = 0.dp,
            snackbarHostState = snackbarHostState,
            onChooseIcon = onChooseIcon,
            onAddUnadaptedApp = onAddUnadaptedApp,
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = MiuixIcons.ChevronBackward,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                    )
                }
            },
            showSnackbarHost = true,
        )
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun RuleListScreenPreview() {
    GlyphTheme {
        val snackbarHostState = remember { SnackbarHostState() }
        RuleListScreen(
            state = RuleListState(),
            onQueryChange = {},
            onRuleEnabledChange = { _, _, _ -> },
            onRuleEnabledAllChange = { _, _, _ -> },
            onInstalledRulesEnabledAllChange = { _, _ -> },
            bottomPadding = 0.dp,
            snackbarHostState = snackbarHostState,
            onChooseIcon = {},
            onAddUnadaptedApp = {},
        )
    }
}
