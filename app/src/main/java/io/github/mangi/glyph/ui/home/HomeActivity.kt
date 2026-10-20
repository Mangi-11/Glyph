package io.github.mangi.glyph.ui.home

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
import io.github.mangi.glyph.core.LauncherIconController
import io.github.mangi.glyph.core.SystemPackages
import io.github.mangi.glyph.diagnostics.AppDiagnostics
import io.github.mangi.glyph.diagnostics.DiagnosticEvent
import io.github.mangi.glyph.diagnostics.DiagnosticLevel
import io.github.mangi.glyph.diagnostics.OccurrencePolicy
import io.github.mangi.glyph.framework.MainThreadCallbacks
import io.github.mangi.glyph.framework.RemoteConfigCoordinator
import io.github.mangi.glyph.framework.RemoteRuleMirror
import io.github.mangi.glyph.framework.SystemUiRestarter
import io.github.mangi.glyph.framework.XposedServiceBridge
import io.github.mangi.glyph.rules.IconRule
import io.github.mangi.glyph.rules.RuleRepository
import io.github.mangi.glyph.rules.RuleStore
import io.github.mangi.glyph.ui.main.GlyphRoot
import io.github.mangi.glyph.ui.rules.InstalledAppChoice
import io.github.mangi.glyph.ui.rules.InstalledPackageInventory
import io.github.mangi.glyph.ui.rules.InstalledPackageSnapshot
import io.github.mangi.glyph.ui.rules.RuleListState
import io.github.mangi.glyph.ui.theme.GlyphAppTheme
import io.github.mangi.glyph.ui.theme.ThemeConfig
import io.github.mangi.glyph.ui.theme.readThemeConfig
import io.github.libxposed.service.XposedService
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong

class HomeActivity : ComponentActivity() {

    private var uiState by mutableStateOf(HomeScreenState())
    private var themeConfig by mutableStateOf(ThemeConfig())
    private var ruleState by mutableStateOf(RuleListState())
    private var currentService: XposedService? = null
    private val loadRequest = AtomicLong()
    private val ruleLoader = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "rule-list-loader")
    }

    private val frameworkListener = object : XposedServiceBridge.Listener {
        override fun onServiceChanged(service: XposedService?) {
            refreshLocalState(currentService = service)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        themeConfig = readThemeConfig(this)
        refreshLocalState(currentService = XposedServiceBridge.getCurrentService())
        loadRules()

        enableEdgeToEdge()
        setContent {
            GlyphAppTheme(themeConfig = themeConfig) {
                GlyphRoot(
                    homeState = uiState,
                    ruleState = ruleState,
                    themeConfig = themeConfig,
                    onThemeConfigChange = { themeConfig = it },
                    onSyncRules = ::syncRules,
                    onRestartSystemUi = ::performRestartSystemUi,
                    onRulesEnabledChange = ::setRulesEnabled,
                    onIconSourceModeChange = ::setIconSourceMode,
                    onPanelIconReplacementEnabledChange = ::setPanelIconReplacementEnabled,
                    onLockScreenCapsuleIconReplacementEnabledChange =
                        ::setLockScreenCapsuleIconReplacementEnabled,
                    onOplusPushSpecialHandlingEnabledChange = ::setOplusPushSpecialHandlingEnabled,
                    onPlaceholderIconEnabledChange = ::setPlaceholderIconEnabled,
                    onLauncherIconHiddenChange = ::setLauncherIconHidden,
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

    private fun refreshLocalState(currentService: XposedService? = this.currentService) {
        val serviceSnapshot = XposedServiceBridge.snapshot(currentService)
        this.currentService = currentService.takeIf { serviceSnapshot != null }
        uiState = uiState.copy(
            frameworkConnection = serviceSnapshot?.let {
                FrameworkConnection(
                    name = it.frameworkName,
                    version = it.frameworkVersion,
                    apiVersion = it.apiVersion,
                    grantedScopes = it.scopes,
                )
            },
            rulesCount = RuleStore.rulesCount,
            enabledRulesCount = enabledLibraryRulesCount(),
            rulesUpdatedAt = RuleStore.rulesUpdatedAt,
            config = RuleStore.moduleConfig,
            launcherIconHidden = LauncherIconController.isHidden(this),
        )
        ruleState = ruleState.copy(
            config = RuleStore.moduleConfig,
            canEditConfig = serviceSnapshot?.scopes?.containsAll(REQUIRED_SCOPES) == true,
        )
    }

    private fun enabledLibraryRulesCount(): Int = try {
        RuleStore.rules.count { it.isLibraryEntry && it.isEnabled }
    } catch (_: Exception) {
        0
    }

    private fun setLauncherIconHidden(hidden: Boolean, onShowMessage: (String) -> Unit) {
        runCatching { LauncherIconController.setHidden(this, hidden) }
            .onSuccess {
                val isHidden = LauncherIconController.isHidden(this)
                uiState = uiState.copy(
                    launcherIconHidden = isHidden,
                )
                if (isHidden) onShowMessage(getString(R.string.message_launcher_icon_hidden))
            }
            .onFailure { exception ->
                onShowMessage(
                    getString(
                        R.string.message_launcher_icon_update_failed,
                        exception.localizedMessage ?: getString(R.string.message_unknown_error),
                    )
                )
            }
    }

    private fun setRulesEnabled(enabled: Boolean, onShowMessage: (String) -> Unit) {
        val service = requireFrameworkService(onShowMessage) ?: return
        updateConfig(service, onShowMessage) { RuleStore.setRulesEnabled(enabled) }
    }

    private fun setIconSourceMode(mode: RuleStore.IconSourceMode, onShowMessage: (String) -> Unit) {
        val service = requireFrameworkService(onShowMessage) ?: return
        updateConfig(service, onShowMessage) { RuleStore.setIconSourceMode(mode) }
    }

    private fun setPanelIconReplacementEnabled(enabled: Boolean, onShowMessage: (String) -> Unit) {
        val service = requireFrameworkService(onShowMessage) ?: return
        updateConfig(service, onShowMessage) { RuleStore.setPanelIconReplacementEnabled(enabled) }
    }

    private fun setLockScreenCapsuleIconReplacementEnabled(
        enabled: Boolean,
        onShowMessage: (String) -> Unit,
    ) {
        val service = requireFrameworkService(onShowMessage) ?: return
        updateConfig(service, onShowMessage) {
            RuleStore.setLockScreenCapsuleIconReplacementEnabled(enabled)
        }
    }

    private fun setOplusPushSpecialHandlingEnabled(enabled: Boolean, onShowMessage: (String) -> Unit) {
        val service = requireFrameworkService(onShowMessage) ?: return
        updateConfig(service, onShowMessage) { RuleStore.setOplusPushSpecialHandlingEnabled(enabled) }
    }

    private fun setPlaceholderIconEnabled(enabled: Boolean, onShowMessage: (String) -> Unit) {
        val service = requireFrameworkService(onShowMessage) ?: return
        updateConfig(service, onShowMessage) { RuleStore.setPlaceholderIconEnabled(enabled) }
    }

    private fun updateConfig(
        service: XposedService,
        onShowMessage: (String) -> Unit,
        mutation: () -> Unit,
    ) {
        val updated = RemoteConfigCoordinator.update(service, mutation) { result ->
            if (result is RemoteRuleMirror.PublishResult.Failed) {
                onShowMessage(
                    getString(R.string.message_settings_apply_failed, result.failure.userMessage)
                )
            }
        }
        if (updated) refreshLocalState()
    }

    private fun syncRules(onShowMessage: (String) -> Unit) {
        requireFrameworkService(onShowMessage) ?: return
        uiState = uiState.copy(syncStage = RuleSyncStage.SyncingRules)
        RuleRepository.syncRules { result ->
            result.onSuccess { syncResult ->
                refreshLocalState()
                val service = currentService
                if (service == null) {
                    uiState = uiState.copy(syncStage = RuleSyncStage.Idle)
                    onShowMessage(
                        getString(R.string.message_rules_update_service_unavailable, syncResult.count)
                    )
                    return@onSuccess
                }
                uiState = uiState.copy(syncStage = RuleSyncStage.MirroringRemote)
                RemoteConfigCoordinator.publish(service) { mirrorResult ->
                    uiState = uiState.copy(syncStage = RuleSyncStage.Idle)
                    val message = when (mirrorResult) {
                        is RemoteRuleMirror.PublishResult.Published ->
                            getString(
                                R.string.message_rules_update_success,
                                syncResult.count
                            )

                        is RemoteRuleMirror.PublishResult.Failed ->
                            getString(
                                R.string.message_rules_update_not_applied,
                                syncResult.count,
                                mirrorResult.failure.userMessage,
                            )
                    }
                    onShowMessage(message)
                }
                loadRules()
            }.onFailure { exception ->
                uiState = uiState.copy(syncStage = RuleSyncStage.Idle)
                onShowMessage(
                    getString(
                        R.string.message_rules_update_failed,
                        (exception as? RuleRepository.SyncFailure)?.userMessage
                            ?: getString(R.string.message_unknown_error)
                    )
                )
            }
        }
    }

    private fun performRestartSystemUi(onShowMessage: (String) -> Unit) {
        val service = currentService
        if (service == null) {
            onShowMessage(getString(R.string.message_service_unavailable))
            return
        }
        RemoteConfigCoordinator.publish(service) { mirrorResult ->
            when (mirrorResult) {
                is RemoteRuleMirror.PublishResult.Published -> restartSystemUiDirectly(onShowMessage)
                is RemoteRuleMirror.PublishResult.Failed -> onShowMessage(
                    getString(R.string.message_settings_not_applied, mirrorResult.failure.userMessage)
                )
            }
        }
    }

    private fun restartSystemUiDirectly(onShowMessage: (String) -> Unit) {
        SystemUiRestarter.restartSystemUi { result ->
            result.onSuccess {
                onShowMessage(getString(R.string.message_restart_requested))
            }.onFailure {
                val userMessage = (it as? SystemUiRestarter.RestartFailure)?.userMessage
                    ?: getString(R.string.message_restart_failed_generic)
                onShowMessage(
                    getString(R.string.message_restart_failed, userMessage)
                )
            }
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

    private fun requireFrameworkService(onShowMessage: (String) -> Unit): XposedService? {
        val service = currentService
        if (service == null) {
            onShowMessage(getString(R.string.message_service_unavailable))
        }
        return service
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
                            ruleState = ruleState.copy(isLoading = false, loadFailed = true)
                        }
                    }
                    return@execute
                }
                val packages = rules.mapTo(linkedSetOf()) { it.packageName }
                val installedPackages = readInstalledPackages(packages)
                val unadaptedApps = readUnadaptedInstalledApps(packages)
                MainThreadCallbacks.dispatch("rule_list_load") {
                    if (request == loadRequest.get() && !isDestroyed) {
                        ruleState = ruleState.copy(
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
                    ruleState = ruleState.copy(isLoading = false, loadFailed = true)
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
        ruleState = ruleState.copy(query = query)
    }

    private fun setRuleEnabled(rule: IconRule, enabled: Boolean, onShowMessage: (String) -> Unit) {
        val service = requireFrameworkService(onShowMessage) ?: return
        val updated = RemoteConfigCoordinator.update(
            service = service,
            mutation = { RuleStore.setRuleEnabled(rule.packageName, enabled) },
        ) { result ->
            showRulePublishFailure(result, onShowMessage)
        }
        if (!updated) return
        ruleState = ruleState.copy(
            rules = ruleState.rules.mapRule(rule.packageName) { it.copy(isEnabled = enabled) },
            config = RuleStore.moduleConfig,
        )
    }

    private fun setRuleEnabledAll(rule: IconRule, enabledAll: Boolean, onShowMessage: (String) -> Unit) {
        val service = requireFrameworkService(onShowMessage) ?: return
        val updated = RemoteConfigCoordinator.update(
            service = service,
            mutation = { RuleStore.setRuleEnabledAll(rule.packageName, enabledAll) },
        ) { result ->
            showRulePublishFailure(result, onShowMessage)
        }
        if (!updated) return
        ruleState = ruleState.copy(
            rules = ruleState.rules.mapRule(rule.packageName) { it.copy(isEnabledAll = enabledAll) },
            config = RuleStore.moduleConfig,
        )
    }

    private fun setInstalledRulesEnabledAll(enabledAll: Boolean, onShowMessage: (String) -> Unit) {
        val service = requireFrameworkService(onShowMessage) ?: return
        val packageNames = ruleState.installedEnabledRulePackageNames
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
                is RemoteRuleMirror.PublishResult.Failed -> showRulePublishFailure(result, onShowMessage)
            }
        }
        if (!updated) return
        ruleState = ruleState.copy(
            rules = ruleState.rules.map { rule ->
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
            showRulePublishFailure(result, onShowMessage)
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
            showRulePublishFailure(result, onShowMessage)
        }
        if (!updated) return
        loadRules()
    }

    private fun showRulePublishFailure(
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

    private fun List<IconRule>.mapRule(
        packageName: String,
        transform: (IconRule) -> IconRule,
    ) = map { rule ->
        if (rule.packageName == packageName) transform(rule) else rule
    }

    override fun onDestroy() {
        loadRequest.incrementAndGet()
        ruleLoader.shutdownNow()
        super.onDestroy()
    }

    companion object {
        private val REQUIRED_SCOPES = setOf(SystemPackages.SYSTEM_SCOPE, SystemPackages.SYSTEM_UI)
    }
}
