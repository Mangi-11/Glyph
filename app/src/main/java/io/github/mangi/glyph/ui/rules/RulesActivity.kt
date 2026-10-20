package io.github.mangi.glyph.ui.rules

import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.mangi.glyph.R
import io.github.mangi.glyph.core.SystemPackages
import io.github.mangi.glyph.diagnostics.AppDiagnostics
import io.github.mangi.glyph.diagnostics.DiagnosticEvent
import io.github.mangi.glyph.diagnostics.DiagnosticLevel
import io.github.mangi.glyph.diagnostics.OccurrencePolicy
import io.github.mangi.glyph.framework.MainThreadCallbacks
import io.github.mangi.glyph.framework.RemoteConfigCoordinator
import io.github.mangi.glyph.framework.RemoteRuleMirror
import io.github.mangi.glyph.framework.XposedServiceBridge
import io.github.mangi.glyph.rules.IconRule
import io.github.mangi.glyph.rules.RuleStore
import io.github.mangi.glyph.ui.theme.GlyphTheme
import io.github.libxposed.service.XposedService
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong

class RulesActivity : ComponentActivity() {

    private var uiState by mutableStateOf(RuleListState())
    private var currentService: XposedService? = null
    private val loadRequest = AtomicLong()
    private val ruleLoader = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "rule-list-loader")
    }

    private val frameworkListener = object : XposedServiceBridge.Listener {
        override fun onServiceChanged(service: XposedService?) {
            refreshFrameworkState(currentService = service)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        refreshFrameworkState(currentService = XposedServiceBridge.getCurrentService())
        loadRules()

        enableEdgeToEdge()
        setContent {
            GlyphTheme {
                StandaloneRuleListScreen(
                    state = uiState,
                    onBack = ::finish,
                    onQueryChange = ::updateQuery,
                    onRuleEnabledChange = ::setRuleEnabled,
                    onRuleEnabledAllChange = ::setRuleEnabledAll,
                    onInstalledRulesEnabledAllChange = ::setInstalledRulesEnabledAll,
                    onRuleIconSourceChange = ::setRuleIconSource,
                    onBindUnadaptedApp = ::bindUnadaptedApp,
                )
            }
        }
    }

    private fun refreshFrameworkState(currentService: XposedService? = this.currentService) {
        val serviceSnapshot = XposedServiceBridge.snapshot(currentService)
        this.currentService = currentService.takeIf { serviceSnapshot != null }
        uiState = uiState.copy(
            config = RuleStore.moduleConfig,
            canEditConfig = serviceSnapshot?.scopes?.containsAll(REQUIRED_SCOPES) == true,
        )
    }

    private fun loadRules() {
        val request = loadRequest.incrementAndGet()
        try {
            ruleLoader.execute {
                val rules = try {
                    RuleStore.rules
                } catch (exception: Exception) {
                    reportRuleLoadFailure(exception)
                    MainThreadCallbacks.dispatch("rule_list_load") {
                        if (request == loadRequest.get() && !isDestroyed) {
                            uiState = uiState.copy(isLoading = false, loadFailed = true)
                        }
                    }
                    return@execute
                }
                val installedPackages = readInstalledPackages(
                    rulePackageNames = rules.mapTo(linkedSetOf()) { it.packageName },
                )
                val unadaptedApps = readUnadaptedInstalledApps(
                    rulePackageNames = rules.mapTo(linkedSetOf()) { it.packageName },
                )
                MainThreadCallbacks.dispatch("rule_list_load") {
                    if (request == loadRequest.get() && !isDestroyed) {
                        uiState = uiState.copy(
                            rules = rules,
                            installedPackageNames = installedPackages.names,
                            installedPackagesKnown = installedPackages.available,
                            unadaptedInstalledApps = unadaptedApps,
                            config = RuleStore.moduleConfig,
                            isLoading = false,
                            loadFailed = false,
                        )
                    }
                }
            }
        } catch (exception: Exception) {
            reportRuleLoadFailure(exception)
            MainThreadCallbacks.dispatch("rule_list_load") {
                if (request == loadRequest.get() && !isDestroyed) {
                    uiState = uiState.copy(isLoading = false, loadFailed = true)
                }
            }
        }
    }

    private fun reportRuleLoadFailure(exception: Exception) {
        AppDiagnostics.logger.report(
            level = DiagnosticLevel.Error,
            event = DiagnosticEvent.RulesLoadFailed,
            message = "Unable to load rules for the management screen",
            cause = exception,
            attributes = mapOf("phase" to "rule_list"),
            occurrence = OccurrencePolicy.Once("rule-list"),
        )
    }

    @Suppress("DEPRECATION")
    private fun readInstalledPackages(rulePackageNames: Set<String>): InstalledPackageSnapshot = try {
        val launcherApps by lazy(LazyThreadSafetyMode.NONE) {
            checkNotNull(getSystemService(LauncherApps::class.java)) {
                "LauncherApps service is unavailable"
            }
        }
        InstalledPackageInventory.collect(
            rulePackageNames = rulePackageNames,
            readCurrentUserPackages = {
                packageManager
                    .getInstalledApplications(PackageManager.MATCH_DISABLED_COMPONENTS)
                    .mapTo(mutableSetOf()) { it.packageName }
            },
            readAccessibleProfiles = { launcherApps.profiles },
            isInstalledForProfile = { packageName, profile ->
                try {
                    launcherApps.getApplicationInfo(
                        packageName,
                        PackageManager.MATCH_DISABLED_COMPONENTS,
                        profile,
                    ) != null
                } catch (_: PackageManager.NameNotFoundException) {
                    false
                }
            },
        )
    } catch (exception: Exception) {
        AppDiagnostics.logger.report(
            level = DiagnosticLevel.Warning,
            event = DiagnosticEvent.InstalledPackagesReadFailed,
            message = "Unable to group rules by installed packages",
            cause = exception,
            occurrence = OccurrencePolicy.Once("rule-list"),
        )
        InstalledPackageSnapshot(names = emptySet(), available = false)
    }

    private fun readUnadaptedInstalledApps(rulePackageNames: Set<String>): List<InstalledAppChoice> = try {
        packageManager
            .getInstalledApplications(PackageManager.MATCH_DISABLED_COMPONENTS)
            .asSequence()
            .filter { it.packageName !in rulePackageNames }
            .map { application ->
                val label = try {
                    packageManager.getApplicationLabel(application).toString()
                        .ifBlank { application.packageName }
                } catch (_: Exception) {
                    application.packageName
                }
                InstalledAppChoice(packageName = application.packageName, label = label)
            }
            .sortedBy { it.label.lowercase(Locale.getDefault()) }
            .toList()
    } catch (exception: Exception) {
        AppDiagnostics.logger.report(
            level = DiagnosticLevel.Warning,
            event = DiagnosticEvent.InstalledPackagesReadFailed,
            message = "Unable to list unadapted installed applications",
            cause = exception,
            occurrence = OccurrencePolicy.Once("rule-list-unadapted"),
        )
        emptyList()
    }

    private fun updateQuery(query: String) {
        uiState = uiState.copy(query = query)
    }

    private fun setRuleEnabled(rule: IconRule, enabled: Boolean, onShowMessage: (String) -> Unit) {
        val service = requireFrameworkService(onShowMessage) ?: return
        val updated = RemoteConfigCoordinator.update(
            service = service,
            mutation = { RuleStore.setRuleEnabled(rule.packageName, enabled) },
        ) { result ->
            showPublishFailure(result, onShowMessage)
        }
        if (!updated) return
        uiState = uiState.copy(
            rules = uiState.rules.mapRule(rule.packageName) { it.copy(isEnabled = enabled) },
            config = RuleStore.moduleConfig,
        )
    }

    private fun setRuleEnabledAll(rule: IconRule, enabledAll: Boolean, onShowMessage: (String) -> Unit) {
        val service = requireFrameworkService(onShowMessage) ?: return
        val updated = RemoteConfigCoordinator.update(
            service = service,
            mutation = { RuleStore.setRuleEnabledAll(rule.packageName, enabledAll) },
        ) { result ->
            showPublishFailure(result, onShowMessage)
        }
        if (!updated) return
        uiState = uiState.copy(
            rules = uiState.rules.mapRule(rule.packageName) { it.copy(isEnabledAll = enabledAll) },
            config = RuleStore.moduleConfig,
        )
    }

    private fun setInstalledRulesEnabledAll(enabledAll: Boolean, onShowMessage: (String) -> Unit) {
        val service = requireFrameworkService(onShowMessage) ?: return
        val packageNames = uiState.installedEnabledRulePackageNames
        if (packageNames.isEmpty()) return
        val updated = RemoteConfigCoordinator.update(
            service = service,
            mutation = { RuleStore.setRulesEnabledAll(packageNames, enabledAll) },
        ) { result ->
            when (result) {
                is RemoteRuleMirror.PublishResult.Published -> onShowMessage(
                    getString(
                        if (enabledAll) {
                            R.string.message_installed_rules_enabled_all
                        } else {
                            R.string.message_installed_rules_disabled_all
                        },
                        packageNames.size,
                    )
                )
                is RemoteRuleMirror.PublishResult.Failed -> showPublishFailure(result, onShowMessage)
            }
        }
        if (!updated) return
        uiState = uiState.copy(
            rules = uiState.rules.map { rule ->
                if (rule.packageName in packageNames) rule.copy(isEnabledAll = enabledAll) else rule
            },
            config = RuleStore.moduleConfig,
        )
    }

    private fun setRuleIconSource(
        rule: IconRule,
        sourcePackage: String?,
        onShowMessage: (String) -> Unit,
    ) {
        val service = requireFrameworkService(onShowMessage) ?: return
        val updated = RemoteConfigCoordinator.update(
            service = service,
            mutation = { RuleStore.setRuleIconSource(rule.packageName, sourcePackage) },
        ) { result ->
            showPublishFailure(result, onShowMessage)
        }
        if (!updated) return
        loadRules()
    }

    private fun bindUnadaptedApp(
        app: InstalledAppChoice,
        sourcePackage: String,
        onShowMessage: (String) -> Unit,
    ) {
        val service = requireFrameworkService(onShowMessage) ?: return
        val updated = RemoteConfigCoordinator.update(
            service = service,
            mutation = {
                RuleStore.setRuleIconSource(
                    targetPackage = app.packageName,
                    sourcePackage = sourcePackage,
                    customName = app.label,
                )
            },
        ) { result ->
            showPublishFailure(result, onShowMessage)
        }
        if (!updated) return
        loadRules()
    }

    private fun showPublishFailure(
        result: RemoteRuleMirror.PublishResult,
        onShowMessage: (String) -> Unit,
    ) {
        if (result is RemoteRuleMirror.PublishResult.Failed) {
            onShowMessage(
                getString(
                    R.string.message_settings_apply_failed,
                    result.failure.userMessage,
                )
            )
        }
    }

    override fun onStart() {
        super.onStart()
        XposedServiceBridge.addListener(frameworkListener)
    }

    override fun onStop() {
        XposedServiceBridge.removeListener(frameworkListener)
        super.onStop()
    }

    override fun onDestroy() {
        loadRequest.incrementAndGet()
        ruleLoader.shutdownNow()
        super.onDestroy()
    }

    private fun requireFrameworkService(onShowMessage: (String) -> Unit): XposedService? {
        val service = currentService
        if (service == null) {
            onShowMessage(getString(R.string.message_service_unavailable))
        }
        return service
    }

    private fun List<IconRule>.mapRule(
        packageName: String,
        transform: (IconRule) -> IconRule,
    ) = map { rule ->
        if (rule.packageName == packageName) transform(rule) else rule
    }

    companion object {
        private val REQUIRED_SCOPES = setOf(SystemPackages.SYSTEM_SCOPE, SystemPackages.SYSTEM_UI)
    }
}
