package io.github.mangi.glyph.hook.systemui

import android.graphics.drawable.Icon
import android.service.notification.StatusBarNotification
import androidx.core.os.BundleCompat
import io.github.mangi.glyph.diagnostics.Diagnostics
import io.github.mangi.glyph.hook.HookContract
import io.github.mangi.glyph.hook.runtimeFailure

internal fun StatusBarNotification.originalSmallIcon(
    diagnostics: Diagnostics,
    revision: Long,
): Icon? {
    val preserved = try {
        BundleCompat.getParcelable(
            notification.extras,
            HookContract.EXTRA_ORIGINAL_SMALL_ICON,
            Icon::class.java,
        )
    } catch (exception: Exception) {
        diagnostics.runtimeFailure(
            scope = "notification:original_small_icon",
            message = "读取修正前的 smallIcon 失败，使用当前通知图标",
            cause = exception,
            revision = revision,
        )
        null
    }
    return preserved ?: notification.smallIcon
}
