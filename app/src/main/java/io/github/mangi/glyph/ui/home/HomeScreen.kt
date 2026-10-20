package io.github.mangi.glyph.ui.home

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.mangi.glyph.BuildConfig
import io.github.mangi.glyph.R
import io.github.mangi.glyph.diagnostics.AppDiagnostics
import io.github.mangi.glyph.diagnostics.DiagnosticEvent
import io.github.mangi.glyph.diagnostics.DiagnosticLevel
import io.github.mangi.glyph.diagnostics.OccurrencePolicy
import io.github.mangi.glyph.ui.component.MiuixBlurredTopBar
import io.github.mangi.glyph.ui.component.PreferenceGroup
import io.github.mangi.glyph.ui.component.rememberMiuixPageBackdrop
import io.github.mangi.glyph.ui.theme.GlyphTheme
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Download
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val SOFTWARE_VERSION_PROPERTY = "ro.build.display.id.show"

@Composable
fun HomeScreen(
    state: HomeScreenState,
    bottomPadding: Dp,
    snackbarHostState: SnackbarHostState,
    onSyncRules: ((String) -> Unit) -> Unit,
    onRestartSystemUi: ((String) -> Unit) -> Unit,
) {
    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberMiuixPageBackdrop()
    val surfaceColor = MiuixTheme.colorScheme.surface
    var showRestartDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun showSnackbar(message: String) {
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    Scaffold(
        topBar = {
            MiuixBlurredTopBar(backdrop) {
                TopAppBar(
                    title = stringResource(R.string.app_name),
                    largeTitle = stringResource(R.string.home_title),
                    color = if (backdrop != null) Color.Transparent else surfaceColor,
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
            item { StatusOverview(state = state) }
            item {
                PreferenceGroup {
                    RulesCard(
                        state = state,
                        onSyncRules = { onSyncRules(::showSnackbar) },
                        onRestartClick = { showRestartDialog = true },
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    RestartDialog(
        show = showRestartDialog,
        onDismiss = { showRestartDialog = false },
        onConfirm = {
            showRestartDialog = false
            onRestartSystemUi(::showSnackbar)
        },
    )
}

@Composable
private fun StatusOverview(state: HomeScreenState) {
    val context = LocalContext.current
    val moduleVersionText = stringResource(
        R.string.status_hero_module_version,
        BuildConfig.VERSION_NAME,
        BuildConfig.VERSION_CODE,
    )
    val softwareVersionText = remember { softwareVersionText() }
    val heroSpec = statusHeroSpec(context, state, moduleVersionText)

    Column(
        modifier = Modifier.padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusHeroCard(
                spec = heroSpec,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) {
                MetricCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    title = stringResource(R.string.home_card_rules),
                    value = state.rulesCount.toString(),
                )
                Spacer(modifier = Modifier.height(12.dp))
                MetricCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    title = stringResource(R.string.home_card_enabled),
                    value = state.enabledRulesCount.toString(),
                )
            }
        }
        SystemInfoCard(
            state = state,
            moduleVersionText = moduleVersionText,
            softwareVersionText = softwareVersionText,
        )
    }
}

@Composable
private fun MetricCard(
    modifier: Modifier,
    title: String,
    value: String,
) {
    Card(
        modifier = modifier,
        insideMargin = PaddingValues(16.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = title,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = value,
                fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold,
                color = MiuixTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun StatusHeroCard(
    spec: StatusHeroSpec,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        insideMargin = PaddingValues(),
        colors = CardDefaults.defaultColors(
            color = spec.containerColor,
            contentColor = spec.accentColor,
        ),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(38.dp, 45.dp),
                contentAlignment = Alignment.BottomEnd,
            ) {
                Icon(
                    imageVector = spec.icon,
                    contentDescription = null,
                    modifier = Modifier.size(170.dp),
                    tint = spec.accentColor,
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            ) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = spec.status,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MiuixTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = spec.versionLine,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MiuixTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun SystemInfoCard(
    state: HomeScreenState,
    moduleVersionText: String,
    softwareVersionText: String,
) {
    val deviceText = remember {
        listOf(Build.BRAND, Build.MODEL)
            .map { it.orEmpty().trim() }
            .filter(String::isNotEmpty)
            .joinToString(" ")
            .ifBlank { Build.DEVICE.orEmpty() }
    }
    val framework = state.frameworkConnection
    val frameworkText = if (framework == null) {
        stringResource(R.string.home_info_framework_disconnected)
    } else {
        "${framework.name} ${framework.version} · API ${framework.apiVersion}"
    }
    val scopesText = when {
        framework == null -> stringResource(R.string.status_hero_inactive_detail)
        state.missingScopes.isNotEmpty() -> stringResource(
            R.string.status_hero_missing_scopes_detail,
            state.missingScopes.joinToString(),
        )
        else -> stringResource(R.string.home_info_scopes_ready)
    }
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            InfoText(
                title = stringResource(R.string.home_info_system_version),
                content = softwareVersionText.ifBlank { deviceText },
            )
            InfoText(
                title = stringResource(R.string.home_info_device),
                content = deviceText,
            )
            InfoText(
                title = stringResource(R.string.home_info_module_version),
                content = moduleVersionText,
            )
            InfoText(
                title = stringResource(R.string.home_info_framework),
                content = frameworkText,
            )
            InfoText(
                title = stringResource(R.string.home_info_scopes),
                content = scopesText,
                bottomPadding = 0.dp,
            )
        }
    }
}

@Composable
private fun InfoText(
    title: String,
    content: String,
    bottomPadding: Dp = 24.dp,
) {
    Text(
        text = title,
        fontSize = MiuixTheme.textStyles.headline1.fontSize,
        fontWeight = FontWeight.Medium,
        color = MiuixTheme.colorScheme.onSurface,
    )
    Text(
        text = content,
        fontSize = MiuixTheme.textStyles.body2.fontSize,
        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        modifier = Modifier.padding(top = 2.dp, bottom = bottomPadding),
    )
}

@Composable
private fun RulesCard(
    state: HomeScreenState,
    onSyncRules: () -> Unit,
    onRestartClick: () -> Unit,
) {
    val lastSyncText = remember(state.rulesUpdatedAt) {
        if (state.rulesUpdatedAt > 0L) {
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(Date(state.rulesUpdatedAt))
        } else {
            null
        }
    }

    BasicComponent(
        title = when (state.syncStage) {
            RuleSyncStage.Idle -> stringResource(R.string.button_sync_rules)
            RuleSyncStage.SyncingRules -> stringResource(R.string.button_syncing_rules)
            RuleSyncStage.MirroringRemote -> stringResource(R.string.button_mirroring_rules)
        },
        summary = if (lastSyncText == null) {
            stringResource(R.string.label_rules_never_synced)
        } else {
            stringResource(R.string.label_rules_last_sync, lastSyncText)
        },
        endActions = if (!state.isSyncing) {
            {
                Icon(
                    imageVector = MiuixIcons.Download,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
                )
            }
        } else null,
        onClick = onSyncRules,
        enabled = !state.isSyncing && state.canEditConfig,
    )
    BasicComponent(
        title = stringResource(R.string.label_restart_systemui),
        endActions = {
            Icon(
                imageVector = MiuixIcons.Refresh,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
            )
        },
        onClick = onRestartClick,
        enabled = state.canEditConfig,
    )
}

@Composable
private fun RestartDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    OverlayDialog(
        show = show,
        title = stringResource(R.string.dialog_restart_title),
        summary = stringResource(R.string.dialog_restart_message),
        onDismissRequest = onDismiss,
    ) {
        Row(horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(
                text = stringResource(R.string.dialog_cancel),
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(20.dp))
            TextButton(
                text = stringResource(R.string.dialog_confirm_restart),
                onClick = onConfirm,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.textButtonColorsPrimary(),
            )
        }
    }
}

@Composable
private fun statusHeroSpec(
    context: android.content.Context,
    state: HomeScreenState,
    moduleVersionText: String,
): StatusHeroSpec {
    val frameworkConnection = state.frameworkConnection
    val dark = isSystemInDarkTheme()
    return when {
        frameworkConnection == null -> {
            StatusHeroSpec(
                status = context.getString(R.string.status_hero_inactive_title),
                versionLine = moduleVersionText,
                accentColor = Color(0xFFFF453A),
                containerColor = if (dark) Color(0xFF3A1A1A) else Color(0xFFFFEBEA),
                icon = StatusIcons.ErrorOutline,
            )
        }
        state.missingScopes.isNotEmpty() -> {
            StatusHeroSpec(
                status = context.getString(R.string.status_hero_missing_scopes_title),
                versionLine = moduleApiText(
                    context = context,
                    moduleVersionText = moduleVersionText,
                    apiVersion = frameworkConnection.apiVersion,
                ),
                accentColor = Color(0xFFFF9500),
                containerColor = if (dark) Color(0xFF3A2A12) else Color(0xFFFFF4E0),
                icon = StatusIcons.ErrorOutline,
            )
        }
        else -> {
            StatusHeroSpec(
                status = context.getString(R.string.status_hero_active_title),
                versionLine = moduleApiText(
                    context = context,
                    moduleVersionText = moduleVersionText,
                    apiVersion = frameworkConnection.apiVersion,
                ),
                accentColor = Color(0xFF36D167),
                containerColor = if (dark) Color(0xFF1A3825) else Color(0xFFDFFAE4),
                icon = StatusIcons.CheckCircleOutline,
            )
        }
    }
}

private fun moduleApiText(
    context: android.content.Context,
    moduleVersionText: String,
    apiVersion: Int,
): String = context.getString(
    R.string.status_hero_module_api,
    moduleVersionText,
    apiVersion,
)

private data class StatusHeroSpec(
    val status: String,
    val versionLine: String,
    val accentColor: Color,
    val containerColor: Color,
    val icon: ImageVector,
)

private fun softwareVersionText(): String = cachedSoftwareVersion

private val cachedSoftwareVersion: String by lazy(::readSoftwareVersion)

@SuppressLint("PrivateApi") // ColorOS-only display property; Build.DISPLAY is the safe fallback.
private fun readSoftwareVersion(): String = try {
    (Class.forName("android.os.SystemProperties")
        .getMethod("get", String::class.java)
        .invoke(null, SOFTWARE_VERSION_PROPERTY) as? String)
        ?.trim()
        .orEmpty()
        .ifBlank { Build.DISPLAY.orEmpty() }
} catch (exception: Exception) {
    AppDiagnostics.logger.report(
        level = DiagnosticLevel.Warning,
        event = DiagnosticEvent.SystemInfoReadFailed,
        message = "Unable to read the ColorOS software version property",
        cause = exception,
        attributes = mapOf("scope" to "software_version"),
        occurrence = OccurrencePolicy.Once("software_version"),
    )
    Build.DISPLAY.orEmpty()
}

@Composable
private fun HomeScreenPreview(state: HomeScreenState) {
    val snackbarHostState = remember { SnackbarHostState() }
    HomeScreen(
        state = state,
        bottomPadding = 0.dp,
        snackbarHostState = snackbarHostState,
        onSyncRules = {},
        onRestartSystemUi = {},
    )
}

@Preview(
    name = "Home Screen Light",
    showBackground = true,
    widthDp = 393,
    heightDp = 852,
)
@Composable
private fun HomeScreenLightPreview() {
    GlyphTheme(darkTheme = false) {
        HomeScreenPreview(
            state = HomeScreenState(
                rulesCount = 128,
                enabledRulesCount = 96,
                rulesUpdatedAt = 1742861100000L,
                syncStage = RuleSyncStage.Idle,
            ),
        )
    }
}

@Preview(
    name = "Home Screen Dark",
    showBackground = true,
    widthDp = 393,
    heightDp = 852,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun HomeScreenDarkPreview() {
    GlyphTheme(darkTheme = true) {
        HomeScreenPreview(
            state = HomeScreenState(
                rulesCount = 256,
                enabledRulesCount = 180,
                rulesUpdatedAt = 1742861400000L,
                syncStage = RuleSyncStage.MirroringRemote,
            ),
        )
    }
}

@Preview(
    name = "Home Screen Empty",
    showBackground = true,
    widthDp = 393,
    heightDp = 852,
)
@Composable
private fun HomeScreenEmptyPreview() {
    GlyphTheme(darkTheme = false) {
        HomeScreenPreview(
            state = HomeScreenState(
                rulesCount = 0,
                enabledRulesCount = 0,
                rulesUpdatedAt = 0L,
                syncStage = RuleSyncStage.Idle,
            ),
        )
    }
}
