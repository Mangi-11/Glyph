package io.github.mangi.glyph.ui.theme

import android.app.Application
import android.content.Context
import android.os.Build
import org.lsposed.hiddenapibypass.HiddenApiBypass

fun applyPredictiveBackCallbackEnabled(context: Context, enabled: Boolean) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return
    val app = context.applicationContext as? Application ?: return
    runCatching {
        HiddenApiBypass.addHiddenApiExemptions(
            "Landroid/content/pm/ApplicationInfo;->setEnableOnBackInvokedCallback"
        )
        setEnableOnBackInvokedCallback(app, enabled)
    }
}

private fun setEnableOnBackInvokedCallback(app: Application, enabled: Boolean) {
    val method = app.applicationInfo.javaClass.getDeclaredMethod(
        "setEnableOnBackInvokedCallback",
        Boolean::class.javaPrimitiveType
    )
    method.isAccessible = true
    method.invoke(app.applicationInfo, enabled)
}
