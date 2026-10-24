package io.github.mangi.glyph.hook

import io.github.mangi.glyph.core.ModuleInfo
import io.github.mangi.glyph.diagnostics.DiagnosticEvent
import io.github.mangi.glyph.diagnostics.DiagnosticLevel
import io.github.mangi.glyph.diagnostics.DiagnosticRecord
import io.github.mangi.glyph.diagnostics.DiagnosticSink
import io.github.mangi.glyph.diagnostics.Diagnostics
import io.github.mangi.glyph.diagnostics.OccurrencePolicy
import com.highcapable.yukihookapi.hook.log.YLog
import com.highcapable.yukihookapi.hook.param.HookChain
import com.highcapable.yukihookapi.hook.param.PackageParam
import java.lang.reflect.Executable

internal class YukiHookDiagnosticSink(
    private val processName: String,
) : DiagnosticSink {
    override fun write(record: DiagnosticRecord) {
        val contextualRecord = record.copy(
            attributes = mapOf("process" to processName) + record.attributes,
        )
        val message = contextualRecord.render()
        val cause = contextualRecord.cause
        when (contextualRecord.level) {
            DiagnosticLevel.Debug -> YLog.debug(message, cause, ModuleInfo.LOG_TAG, YLog.EnvType.XPOSED)
            DiagnosticLevel.Info -> YLog.info(message, cause, ModuleInfo.LOG_TAG, YLog.EnvType.XPOSED)
            DiagnosticLevel.Warning -> YLog.warn(message, cause, ModuleInfo.LOG_TAG, YLog.EnvType.XPOSED)
            DiagnosticLevel.Error -> YLog.error(message, cause, ModuleInfo.LOG_TAG, YLog.EnvType.XPOSED)
        }
    }
}

internal class HookRegistrar(
    private val packageParam: PackageParam,
    private val diagnostics: Diagnostics,
    private val processName: String,
) {
    /** YukiHook isolates registration failures and protects the host from callback failures. */
    fun install(
        executable: Executable,
        id: String,
        interceptor: (HookChain) -> Any?,
    ) {
        try {
            with(packageParam) {
                executable.intercept { interceptor(this) }
                    .onCallbackFailure { _, cause ->
                        diagnostics.runtimeFailure(
                            scope = "$processName:$id",
                            message = "Hook 回调失败，交回宿主调用链",
                            cause = cause,
                        )
                    }
            }
            // Registration failures are reported synchronously by YukiHook itself; its handle
            // does not expose a success flag, so do not emit an unconditional success event.
        } catch (exception: Exception) {
            diagnostics.installationFailure(
                scope = "$processName:$id",
                message = "Hook 初始化失败",
                cause = exception,
            )
        }
    }
}

internal fun Diagnostics.memberMissing(
    scope: String,
    message: String,
    cause: Throwable? = null,
) {
    report(
        level = DiagnosticLevel.Warning,
        event = DiagnosticEvent.MemberMissing,
        message = message,
        cause = cause,
        attributes = mapOf("scope" to scope),
        occurrence = OccurrencePolicy.Once(scope),
    )
}

internal fun Diagnostics.runtimeFailure(
    scope: String,
    message: String,
    cause: Throwable,
    revision: Long? = null,
) {
    report(
        level = DiagnosticLevel.Error,
        event = DiagnosticEvent.HookRuntimeFailed,
        message = message,
        cause = cause,
        attributes = buildMap {
            put("scope", scope)
            revision?.let { put("revision", it) }
        },
        occurrence = OccurrencePolicy.RateLimited(scope, RUNTIME_LOG_INTERVAL_MILLIS),
    )
}

internal fun Diagnostics.installationFailure(
    scope: String,
    message: String,
    cause: Exception,
) {
    report(
        level = DiagnosticLevel.Error,
        event = DiagnosticEvent.HookInstallFailed,
        message = message,
        cause = cause,
        attributes = mapOf("scope" to scope),
        occurrence = OccurrencePolicy.Once(scope),
    )
}

private const val RUNTIME_LOG_INTERVAL_MILLIS = 60_000L
