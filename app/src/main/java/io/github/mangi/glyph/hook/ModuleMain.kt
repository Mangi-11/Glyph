package io.github.mangi.glyph.hook

import android.content.SharedPreferences
import com.highcapable.yukihookapi.YukiHook
import com.highcapable.yukihookapi.annotation.xposed.YukiHookLibXposedEntry
import com.highcapable.yukihookapi.hook.param.PackageParam
import com.highcapable.yukihookapi.hook.xposed.YukiHookXposedModule
import com.highcapable.yukihookapi.hook.xposed.bridge.event.registerFrameworkEvents
import com.highcapable.yukihookapi.hook.xposed.bridge.event.v101.onModuleLoaded
import io.github.mangi.glyph.BuildConfig
import io.github.mangi.glyph.core.ModuleInfo
import io.github.mangi.glyph.core.SystemPackages
import io.github.mangi.glyph.diagnostics.DiagnosticEvent
import io.github.mangi.glyph.diagnostics.DiagnosticLevel
import io.github.mangi.glyph.diagnostics.Diagnostics
import io.github.mangi.glyph.diagnostics.OccurrencePolicy
import io.github.mangi.glyph.hook.systemserver.SystemServerHooks
import io.github.mangi.glyph.hook.systemui.SystemUiRuntime
import io.github.mangi.glyph.rules.RuleStore

@YukiHookLibXposedEntry(
    minApiVersion = 102,
    targetApiVersion = 102,
    scope = ["system", SystemPackages.SYSTEM_UI],
    staticScope = true,
)
object ModuleMain : YukiHookXposedModule {

    private var target = ProcessTarget.Ignore
    private var installed = false
    private lateinit var diagnostics: Diagnostics
    private lateinit var openConfigPreferences: () -> SharedPreferences

    override fun onInit() {
        YukiHook.configure {
            debug = BuildConfig.DEBUG
            dataChannel = false
            logging { tag = ModuleInfo.LOG_TAG }
        }
        registerFrameworkEvents {
            onModuleLoaded { param ->
                target = resolveProcessTarget(param.isSystemServer, param.processName)
                diagnostics = Diagnostics(YukiHookDiagnosticSink(param.processName))
                // Native SharedPreferences retains the revision listener used by our publish protocol.
                openConfigPreferences = { getRemotePreferences(RuleStore.GROUP_CONFIG) }
                diagnostics.report(
                    level = DiagnosticLevel.Info,
                    event = DiagnosticEvent.ModuleLoaded,
                    message = "模块入口已路由",
                    attributes = mapOf(
                        "api" to YukiHook.Status.Executor.apiVersion,
                        "framework" to YukiHook.Status.Executor.name,
                        "target" to target.diagnosticName,
                    ),
                    occurrence = OccurrencePolicy.Once(param.processName),
                )
            }
        }
    }

    override fun onHook() = YukiHook.encase {
        loadSystem {
            if (target == ProcessTarget.SystemServer) installTarget()
        }
        loadApp(SystemPackages.SYSTEM_UI) {
            if (target == ProcessTarget.SystemUiMain && isFirstApplication) installTarget()
        }
    }

    private fun PackageParam.installTarget() {
        if (installed) return
        installed = true
        try {
            val loader = requireNotNull(hostClassLoader) { "Host ClassLoader is unavailable" }
            val hooks = HookRegistrar(this, diagnostics, processName)
            when (target) {
                ProcessTarget.SystemServer -> SystemServerHooks(hooks, diagnostics).install(loader)
                ProcessTarget.SystemUiMain -> SystemUiRuntime(
                    openConfigPreferences = openConfigPreferences,
                    sharedFiles = sharedFiles,
                    hooks = hooks,
                    diagnostics = diagnostics,
                    processName = processName,
                ).install(loader)
                ProcessTarget.Ignore -> Unit
            }
        } catch (exception: Exception) {
            diagnostics.installationFailure(
                scope = "${target.diagnosticName}:initialize",
                message = "进程 Hook 初始化失败：${target.diagnosticName}",
                cause = exception,
            )
        }
        // The generated entry owns lifecycle dispatch. Hot reload stays disabled (the default):
        // our hooks, config listeners and delayed UI work have a process lifetime.
    }
}

internal enum class ProcessTarget(val diagnosticName: String) {
    SystemServer("system_server"),
    SystemUiMain("systemui_main"),
    Ignore("ignore"),
}

internal fun resolveProcessTarget(
    isSystemServer: Boolean,
    processName: String,
): ProcessTarget = when {
    isSystemServer -> ProcessTarget.SystemServer
    processName == SystemPackages.SYSTEM_UI -> ProcessTarget.SystemUiMain
    else -> ProcessTarget.Ignore
}
