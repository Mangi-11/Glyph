package io.github.mangi.glyph.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.mangi.glyph.ui.component.blur.BlurredBar
import io.github.mangi.glyph.ui.component.blur.rememberBlurBackdrop
import top.yukonga.miuix.kmp.blur.LayerBackdrop

/** Miuix 页面顶栏共用的 backdrop，结构与 Mishka 的一级页面保持一致。 */
@Composable
fun rememberMiuixPageBackdrop(): LayerBackdrop? = rememberBlurBackdrop()

/** 使用 Miuix 官方 textureBlur 为页面顶栏采样内容。 */
@Composable
fun MiuixBlurredTopBar(
    backdrop: LayerBackdrop?,
    content: @Composable () -> Unit,
) {
    BlurredBar(
        backdrop = backdrop,
        blurActive = backdrop != null,
        content = content,
    )
}
