package io.github.mangi.glyph.ui.rules

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.intl.Locale as ComposeLocale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.mangi.glyph.R
import io.github.mangi.glyph.diagnostics.AppDiagnostics
import io.github.mangi.glyph.diagnostics.DiagnosticEvent
import io.github.mangi.glyph.diagnostics.DiagnosticLevel
import io.github.mangi.glyph.diagnostics.OccurrencePolicy
import io.github.mangi.glyph.rules.IconAsset
import io.github.mangi.glyph.rules.IconRule
import io.github.mangi.glyph.rules.RuleDefinition
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
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ChevronBackward
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
internal fun IconPickerScreen(
    targetAppName: String,
    targetPackageName: String,
    currentSourcePackage: String?,
    libraryRules: List<IconRule>,
    canClear: Boolean,
    onSelect: (IconRule) -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var searchExpanded by remember { mutableStateOf(false) }
    val scrollBehavior = MiuixScrollBehavior(rememberTopAppBarState())
    val locale = ComposeLocale.current.platformLocale
    val keyword = query.trim().lowercase(locale)
    val visibleRules = remember(libraryRules, keyword, locale) {
        if (keyword.isBlank()) {
            libraryRules
        } else {
            libraryRules.filter { rule ->
                rule.definition.appName.lowercase(locale).contains(keyword) ||
                    rule.packageName.lowercase(locale).contains(keyword)
            }
        }
    }
    val selectedPackage = currentSourcePackage
        ?: targetPackageName.takeIf { packageName ->
            libraryRules.any { it.packageName == packageName }
        }

    Scaffold(
        topBar = {
            TopAppBar(
                title = stringResource(R.string.icon_picker_title),
                largeTitle = targetAppName.ifBlank { targetPackageName },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = MiuixIcons.ChevronBackward,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                },
                actions = {
                    if (canClear) {
                        TextButton(
                            text = stringResource(R.string.label_rule_clear_icon),
                            onClick = onClear,
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { paddingValues ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 88.dp),
            modifier = Modifier
                .fillMaxSize()
                .scrollEndHaptic()
                .overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(
                start = 12.dp,
                end = 12.dp,
                top = paddingValues.calculateTopPadding(),
                bottom = paddingValues.calculateBottomPadding() + 24.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                SearchBar(
                    modifier = Modifier.padding(bottom = SearchBarDefaults.InsideMargin.width),
                    inputField = {
                        InputField(
                            query = query,
                            onQueryChange = { query = it },
                            onSearch = { searchExpanded = false },
                            expanded = searchExpanded,
                            onExpandedChange = { searchExpanded = it },
                            label = stringResource(R.string.icon_picker_search_hint),
                        )
                    },
                    onExpandedChange = { searchExpanded = it },
                    expanded = searchExpanded,
                    outsideEndAction = {
                        TextButton(
                            text = stringResource(R.string.dialog_cancel),
                            modifier = Modifier.padding(end = SearchBarDefaults.InsideMargin.width),
                            onClick = {
                                query = ""
                                searchExpanded = false
                            },
                        )
                    },
                ) {}
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                SmallTitle(text = stringResource(R.string.icon_picker_hint))
            }
            if (visibleRules.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyPickerCard(text = stringResource(R.string.icon_picker_empty))
                }
            } else {
                items(
                    items = visibleRules,
                    key = { it.packageName },
                ) { rule ->
                    IconPickerCell(
                        definition = rule.definition,
                        selected = rule.packageName == selectedPackage,
                        onClick = {
                            if (rule.packageName == targetPackageName) onClear() else onSelect(rule)
                        },
                    )
                }
            }
        }
    }
}

@Composable
internal fun InstalledAppPickerScreen(
    apps: List<InstalledAppChoice>,
    onSelect: (InstalledAppChoice) -> Unit,
    onBack: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var searchExpanded by remember { mutableStateOf(false) }
    val scrollBehavior = MiuixScrollBehavior(rememberTopAppBarState())
    val locale = ComposeLocale.current.platformLocale
    val keyword = query.trim().lowercase(locale)
    val visibleApps = remember(apps, keyword, locale) {
        if (keyword.isBlank()) {
            apps
        } else {
            apps.filter { app ->
                app.label.lowercase(locale).contains(keyword) ||
                    app.packageName.lowercase(locale).contains(keyword)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = stringResource(R.string.unadapted_app_picker_title),
                largeTitle = stringResource(R.string.unadapted_app_picker_title),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = MiuixIcons.ChevronBackward,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .scrollEndHaptic()
                .overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = paddingValues,
        ) {
            item {
                SearchBar(
                    modifier = Modifier.padding(bottom = SearchBarDefaults.InsideMargin.width),
                    inputField = {
                        InputField(
                            query = query,
                            onQueryChange = { query = it },
                            onSearch = { searchExpanded = false },
                            expanded = searchExpanded,
                            onExpandedChange = { searchExpanded = it },
                            label = stringResource(R.string.unadapted_app_picker_search_hint),
                        )
                    },
                    onExpandedChange = { searchExpanded = it },
                    expanded = searchExpanded,
                    outsideEndAction = {
                        TextButton(
                            text = stringResource(R.string.dialog_cancel),
                            modifier = Modifier.padding(end = SearchBarDefaults.InsideMargin.width),
                            onClick = {
                                query = ""
                                searchExpanded = false
                            },
                        )
                    },
                ) {}
            }
            if (visibleApps.isEmpty()) {
                item {
                    EmptyPickerCard(text = stringResource(R.string.unadapted_app_picker_empty))
                }
            } else {
                items(
                    items = visibleApps,
                    key = { it.packageName },
                ) { app ->
                    Card(
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .padding(bottom = 10.dp),
                        insideMargin = PaddingValues(0.dp),
                    ) {
                        BasicComponent(
                            title = app.label.ifBlank { app.packageName },
                            summary = app.packageName,
                            onClick = { onSelect(app) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IconPickerCell(
    definition: RuleDefinition,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = if (selected) {
        MiuixTheme.colorScheme.primary
    } else {
        Color.Transparent
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CatalogIcon(
            asset = definition.icon,
            iconColor = definition.iconColor,
            packageName = definition.packageName,
            selectedBorder = borderColor,
        )
        Text(
            text = definition.appName.ifBlank { definition.packageName },
            modifier = Modifier.padding(top = 8.dp),
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
internal fun CatalogIcon(
    asset: IconAsset,
    iconColor: Int,
    packageName: String,
    selectedBorder: Color = Color.Transparent,
    size: Int = 42,
) {
    val tint = if (iconColor != 0) {
        Color(iconColor)
    } else {
        MiuixTheme.colorScheme.onSurfaceVariantSummary
    }
    val imageBitmap = remember(asset) {
        try {
            asset.bitmap.asImageBitmap()
        } catch (exception: Exception) {
            AppDiagnostics.logger.report(
                level = DiagnosticLevel.Error,
                event = DiagnosticEvent.IconDecodeFailed,
                message = "Unable to decode rule icon",
                cause = exception,
                attributes = mapOf(
                    "scope" to "icon_picker",
                    "package" to packageName,
                ),
                occurrence = OccurrencePolicy.Once("picker:$packageName"),
            )
            null
        }
    }
    Box(
        modifier = Modifier
            .size(size.dp)
            .then(
                if (selectedBorder == Color.Transparent) {
                    Modifier
                } else {
                    Modifier.border(2.dp, selectedBorder, CircleShape)
                }
            )
            .clip(CircleShape)
            .background(MiuixTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        imageBitmap?.let {
            Image(
                bitmap = it,
                contentDescription = null,
                modifier = Modifier.size((size * 24 / 42).dp),
                colorFilter = ColorFilter.tint(tint),
            )
        }
    }
}

@Composable
private fun EmptyPickerCard(text: String) {
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
                text = text,
                style = MiuixTheme.textStyles.body1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
    }
}
