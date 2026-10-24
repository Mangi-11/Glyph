package io.github.mangi.glyph

import com.highcapable.yukihookapi.hook.xposed.application.ModuleApplication
import io.github.mangi.glyph.framework.RemoteConfigCoordinator
import io.github.mangi.glyph.framework.XposedServiceBridge
import io.github.mangi.glyph.rules.RuleStore
import io.github.mangi.glyph.ui.theme.applyPredictiveBackCallbackEnabled
import io.github.mangi.glyph.ui.theme.readThemeConfig
import io.github.libxposed.service.XposedService

class GlyphApplication : ModuleApplication() {

    private val configPublisher = object : XposedServiceBridge.Listener {
        override fun onServiceChanged(service: XposedService?) {
            service?.let { RemoteConfigCoordinator.publish(it) }
        }
    }

    override fun onCreate() {
        super.onCreate()
        applyPredictiveBackCallbackEnabled(
            context = this,
            enabled = readThemeConfig(this).predictiveBackToHomeEnabled,
        )
        RuleStore.initialize(this)
        XposedServiceBridge.initialize()
        XposedServiceBridge.addListener(configPublisher)
    }
}
