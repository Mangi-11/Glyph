package io.github.mangi.glyph.ui.about

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import io.github.mangi.glyph.update.UpdateChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.mangi.glyph.BuildConfig
import io.github.mangi.glyph.R
import io.github.mangi.glyph.core.ModuleInfo
import io.github.mangi.glyph.ui.component.blur.BlurredBar
import io.github.mangi.glyph.ui.component.blur.ColorBlendToken
import io.github.mangi.glyph.ui.component.blur.rememberBlurBackdrop
import io.github.mangi.glyph.ui.component.blur.rememberBlurEnabled
import io.github.mangi.glyph.ui.component.effect.BgEffectBackground
import io.github.mangi.glyph.ui.theme.LocalAppDarkMode
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurBlendMode
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun AboutDetailsScreen(
    onBack: () -> Unit = {},
    onOpenUrl: (String) -> Unit = {},
) {
    val scrollBehavior = MiuixScrollBehavior()
    val lazyListState = rememberLazyListState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var checkingUpdate by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var updateHasNew by remember { mutableStateOf(false) }
    var updateMessage by remember { mutableStateOf("") }
    var updateReleaseUrl by remember { mutableStateOf(ModuleInfo.RELEASES_PAGE) }

    fun checkUpdate() {
        if (checkingUpdate) return
        checkingUpdate = true
        scope.launch {
            val result = withContext(Dispatchers.IO) { UpdateChecker.check() }
            checkingUpdate = false
            updateHasNew = result.hasUpdate
            updateMessage = result.message
            updateReleaseUrl = result.releaseUrl
            showUpdateDialog = true
        }
    }

    // 滚动进度是帧率级 State：保持 State 形式，读点限制在 topBar 与各 graphicsLayer，
    // 组合期以值读取会让整个 AboutContent 每帧重组
    val scrollProgressState = remember {
        derivedStateOf {
            when {
                lazyListState.firstVisibleItemIndex > 0 -> 1f

                else -> {
                    val spacer = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == "logoSpacer" }
                    if (spacer != null && spacer.size > 0) {
                        (lazyListState.firstVisibleItemScrollOffset.toFloat() / spacer.size).coerceIn(0f, 1f)
                    } else {
                        0f
                    }
                }
            }
        }
    }
    // 布尔/夹紧后再 derive，等值去重让顶栏只在阈值区间内重组
    val heroCollapsed by remember { derivedStateOf { scrollProgressState.value == 1f } }
    val titleAlpha by remember {
        derivedStateOf { ((scrollProgressState.value - 0.35f) / 0.65f).coerceIn(0f, 1f) }
    }

    val backdrop = rememberBlurBackdrop()
    val blurActive = backdrop != null && heroCollapsed
    val barColor = if (heroCollapsed && !blurActive) colorScheme.surface else Color.Transparent

    Scaffold(
        topBar = {
            BlurredBar(backdrop = backdrop, blurActive = blurActive) {
                SmallTopAppBar(
                    title = stringResource(R.string.about_title),
                    scrollBehavior = scrollBehavior,
                    color = barColor,
                    titleColor = colorScheme.onSurface.copy(alpha = titleAlpha),
                    defaultWindowInsetsPadding = false,
                    navigationIcon = {
                        val layoutDirection = LocalLayoutDirection.current
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = MiuixIcons.Back,
                                contentDescription = stringResource(R.string.common_back),
                                tint = colorScheme.onSurface,
                                modifier = Modifier.graphicsLayer {
                                    scaleX = if (layoutDirection == LayoutDirection.Rtl) -1f else 1f
                                },
                            )
                        }
                    },
                )
            }
        },
        contentWindowInsets = WindowInsets.systemBars
            .add(WindowInsets.displayCutout)
            .only(WindowInsetsSides.Horizontal),
    ) { innerPadding ->
        Box(modifier = if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier) {
            AboutContent(
                innerPadding = innerPadding,
                scrollBehavior = scrollBehavior,
                lazyListState = lazyListState,
                scrollProgress = { scrollProgressState.value },
                checkingUpdate = checkingUpdate,
                onCheckUpdate = ::checkUpdate,
                onOpenUrl = onOpenUrl,
            )
        }
    }

    UpdateDialog(
        show = showUpdateDialog,
        updateHasNew = updateHasNew,
        updateMessage = updateMessage,
        updateReleaseUrl = updateReleaseUrl,
        onDismiss = { showUpdateDialog = false },
        onOpenUrl = onOpenUrl,
    )
}

@Composable
private fun AboutContent(
    innerPadding: PaddingValues,
    scrollBehavior: ScrollBehavior,
    lazyListState: LazyListState,
    scrollProgress: () -> Float,
    checkingUpdate: Boolean,
    onCheckUpdate: () -> Unit,
    onOpenUrl: (String) -> Unit,
) {
    val layoutDirection = LocalLayoutDirection.current
    val density = LocalDensity.current

    val backdrop = rememberLayerBackdrop()

    val isDark = LocalAppDarkMode.current
    val blurEnabled by rememberBlurEnabled()
    val effectBackground = remember(blurEnabled) { isRuntimeShaderSupported() && blurEnabled }

    val cardBlendColors = remember(isDark) {
        if (isDark) ColorBlendToken.Overlay_Thin_Light
        else ColorBlendToken.Pured_Regular_Light
    }
    val logoBlend = remember(isDark) {
        if (isDark) {
            listOf(
                BlendColorEntry(Color(0xe6a1a1a1.toInt()), BlurBlendMode.ColorDodge),
                BlendColorEntry(Color(0x4de6e6e6), BlurBlendMode.LinearLight),
                BlendColorEntry(Color(0xff1af500.toInt()), BlurBlendMode.Lab),
            )
        } else {
            listOf(
                BlendColorEntry(Color(0xcc4a4a4a.toInt()), BlurBlendMode.ColorBurn),
                BlendColorEntry(Color(0xff4f4f4f.toInt()), BlurBlendMode.LinearLight),
                BlendColorEntry(Color(0xff1af200.toInt()), BlurBlendMode.Lab),
            )
        }
    }

    var logoHeightDp by remember { mutableStateOf(300.dp) }

    // 各段淡出进度在 graphicsLayer 内计算，读点全部落在绘制阶段
    val versionCodeProgress = { ((scrollProgress() - 0.05f) / 0.15f).coerceIn(0f, 1f) }
    val projectNameProgress = { ((scrollProgress() - 0.20f) / 0.15f).coerceIn(0f, 1f) }

    val scrollPadding = PaddingValues(
        top = innerPadding.calculateTopPadding(),
        start = innerPadding.calculateStartPadding(layoutDirection),
        end = innerPadding.calculateEndPadding(layoutDirection),
    )
    val logoPadding = PaddingValues(
        top = innerPadding.calculateTopPadding() + 40.dp,
        start = innerPadding.calculateStartPadding(layoutDirection),
        end = innerPadding.calculateEndPadding(layoutDirection),
    )

    BgEffectBackground(
        dynamicBackground = effectBackground,
        modifier = Modifier.fillMaxSize(),
        bgModifier = Modifier.layerBackdrop(backdrop),
        isFullSize = true,
        effectBackground = effectBackground,
        alpha = { 1f - scrollProgress() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = logoPadding.calculateTopPadding() + 52.dp,
                    start = logoPadding.calculateStartPadding(layoutDirection),
                    end = logoPadding.calculateEndPadding(layoutDirection),
                )
                .onSizeChanged { size ->
                    with(density) { logoHeightDp = size.height.toDp() }
                },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                modifier = Modifier
                    .padding(bottom = 5.dp)
                    .graphicsLayer {
                        val p = projectNameProgress()
                        alpha = 1 - p
                        scaleX = 1 - (p * 0.05f)
                        scaleY = 1 - (p * 0.05f)
                    }
                    .then(
                        if (blurEnabled) {
                            Modifier.textureBlur(
                                backdrop = backdrop,
                                shape = RoundedCornerShape(16.dp),
                                blurRadius = 150f,
                                colors = BlurColors(blendColors = logoBlend),
                                contentBlendMode = BlendMode.DstIn,
                                enabled = true,
                            )
                        } else Modifier
                    ),
                text = stringResource(R.string.app_name),
                color = colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                fontSize = 35.sp,
            )
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        val p = versionCodeProgress()
                        alpha = 1 - p
                        scaleX = 1 - (p * 0.05f)
                        scaleY = 1 - (p * 0.05f)
                    },
                color = colorScheme.onSurfaceVariantSummary,
                text = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )
        }

        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .scrollEndHaptic()
                .overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(
                top = scrollPadding.calculateTopPadding(),
                start = scrollPadding.calculateStartPadding(layoutDirection),
                end = scrollPadding.calculateEndPadding(layoutDirection),
            ),
        ) {
            item(key = "logoSpacer") {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(
                            logoHeightDp + 52.dp + logoPadding.calculateTopPadding() - scrollPadding.calculateTopPadding() + 126.dp,
                        ),
                    contentAlignment = Alignment.TopCenter,
                    content = { },
                )
            }

            item(key = "about") {
                Box {
                    Spacer(Modifier.fillParentMaxHeight())
                    Column(
                        modifier = Modifier.padding(bottom = 12.dp),
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp)
                                .padding(bottom = 12.dp)
                                .then(
                                    if (blurEnabled) {
                                        Modifier.textureBlur(
                                            backdrop = backdrop,
                                            shape = RoundedCornerShape(16.dp),
                                            blurRadius = 60f,
                                            colors = BlurColors(blendColors = cardBlendColors),
                                            enabled = true,
                                        )
                                    } else Modifier
                                ),
                            colors = CardDefaults.defaultColors(
                                if (blurEnabled) Color.Transparent else colorScheme.surfaceContainer,
                                Color.Transparent,
                            ),
                        ) {
                            ArrowPreference(
                                title = stringResource(R.string.about_check_update),
                                summary = stringResource(
                                    if (checkingUpdate) {
                                        R.string.about_check_update_checking
                                    } else {
                                        R.string.about_check_update_summary
                                    },
                                ),
                                onClick = onCheckUpdate,
                            )
                        }
                        SmallTitle(text = stringResource(R.string.about_open_source))
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp)
                                .padding(bottom = 12.dp)
                                .then(
                                    if (blurEnabled) {
                                        Modifier.textureBlur(
                                            backdrop = backdrop,
                                            shape = RoundedCornerShape(16.dp),
                                            blurRadius = 60f,
                                            colors = BlurColors(blendColors = cardBlendColors),
                                            enabled = true,
                                        )
                                    } else Modifier
                                ),
                            colors = CardDefaults.defaultColors(
                                if (blurEnabled) Color.Transparent else colorScheme.surfaceContainer,
                                Color.Transparent,
                            ),
                        ) {
                            val ossProjects = remember {
                                listOf(
                                    "Glyph" to ModuleInfo.PROJECT_URL,
                                    "ColorOSNotifyIcon" to "https://github.com/fankes/ColorOSNotifyIcon",
                                    "miuix" to "https://github.com/compose-miuix-ui/miuix",
                                    "Android Notification Icon Project" to ModuleInfo.ANIP_PROJECT_URL,
                                )
                            }
                            ossProjects.forEach { (name, url) ->
                                ArrowPreference(
                                    title = name,
                                    summary = url.removePrefix("https://"),
                                    onClick = { onOpenUrl(url) },
                                )
                            }
                        }

                        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.systemBars))
                    }
                }
            }
        }
    }
}

@Composable
private fun UpdateDialog(
    show: Boolean,
    updateHasNew: Boolean,
    updateMessage: String,
    updateReleaseUrl: String,
    onDismiss: () -> Unit,
    onOpenUrl: (String) -> Unit,
) {
    OverlayDialog(
        show = show,
        title = stringResource(R.string.about_update_dialog_title),
        onDismissRequest = onDismiss,
    ) {
        Text(
            text = updateMessage,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 220.dp)
                .verticalScroll(rememberScrollState()),
        )
        Spacer(modifier = Modifier.height(12.dp))
        if (updateHasNew) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TextButton(
                    text = stringResource(R.string.about_update_ok),
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    text = stringResource(R.string.about_update_open),
                    onClick = {
                        onDismiss()
                        onOpenUrl(updateReleaseUrl)
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                )
            }
        } else {
            TextButton(
                text = stringResource(R.string.about_update_ok),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColorsPrimary(),
            )
        }
    }
}
