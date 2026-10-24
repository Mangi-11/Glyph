package io.github.mangi.glyph.hook.systemui

import android.content.SharedPreferences
import android.service.notification.StatusBarNotification
import io.github.mangi.glyph.diagnostics.Diagnostics
import io.github.mangi.glyph.hook.HookRegistrar
import io.github.mangi.glyph.hook.icon.OplusIconConfigurationReader
import io.github.mangi.glyph.hook.icon.ThemeIconProvider
import io.github.mangi.glyph.hook.installationFailure
import io.github.mangi.glyph.hook.memberMissing
import io.github.mangi.glyph.hook.runtimeFailure
import com.highcapable.yukihookapi.hook.xposed.file.YukiHookSharedFiles

internal class SystemUiRuntime(
    private val openConfigPreferences: () -> SharedPreferences,
    private val sharedFiles: YukiHookSharedFiles,
    private val hooks: HookRegistrar,
    private val diagnostics: Diagnostics,
    private val processName: String,
) {
    fun install(classLoader: ClassLoader) {
        val iconConfiguration = OplusIconConfigurationReader(diagnostics)
        val themeIcons = ThemeIconProvider(diagnostics, iconConfiguration)
        val configuration = SystemUiConfiguration(
            openConfigPreferences = openConfigPreferences,
            sharedFiles = sharedFiles,
            diagnostics = diagnostics,
            processName = processName,
            themeIcons = themeIcons,
            iconConfiguration = iconConfiguration,
        )
        var refreshBridge: NotificationRefreshBridge? = null

        // Register before reflection discovery so no committed revision can be missed during setup.
        configuration.start { snapshot ->
            refreshBridge?.let { refresh ->
                runPublishedTask(snapshot, "notification_refresh") {
                    refresh.refresh(snapshot.revision)
                }
            }
        }

        installFeature("refresh") {
            refreshBridge = NotificationRefreshBridge(
                hooks = hooks,
                diagnostics = diagnostics,
                members = SystemUiMembers.resolveRefresh(classLoader),
            ).also(NotificationRefreshBridge::install)
        }

        installFeature("use_app_icon") {
            val utilityPredicate = SystemUiMembers.resolveUseAppIcon(classLoader)
            utilityPredicate?.let { method ->
                hooks.install(method, "systemui.useAppIconForSmallIcon") { chain ->
                    if (configuration.snapshot.resolver.shouldKeepHostAppIconBehavior()) {
                        chain.proceed()
                    } else {
                        false
                    }
                }
            } ?: diagnostics.memberMissing(
                scope = "systemui:use_app_icon",
                message = "未找到 useAppIconForSmallIcon(Notification): boolean，跳过 ColorOS 应用图标策略",
            )

            val entryMembers = SystemUiMembers.resolveEntryUseAppIcon(classLoader)
            if (utilityPredicate != null && entryMembers != null) {
                hooks.install(
                    entryMembers.predicate,
                    "systemui.useAppIconForSmallIcon.entry",
                ) { chain ->
                    val extension = chain.instanceOrNull ?: return@install chain.proceed()
                    val currentResult = try {
                        val entry = entryMembers.getBase.invoke(extension)
                        val sbn = entry?.let {
                            entryMembers.notificationEntryGetSbn.invoke(it) as? StatusBarNotification
                        }
                        utilityPredicate.invoke(null, sbn?.notification) as? Boolean
                    } catch (exception: Exception) {
                        diagnostics.runtimeFailure(
                            scope = "systemui:use_app_icon:entry",
                            message = "读取通知当前应用图标策略失败，交回 ColorOS 缓存结果",
                            cause = exception,
                            revision = configuration.snapshot.revision,
                        )
                        null
                    }
                    currentResult ?: chain.proceed()
                }
            } else {
                diagnostics.memberMissing(
                    scope = "systemui:use_app_icon:entry",
                    message = "未找到 OplusNotificationEntryExImpl 应用图标判定成员，现有通知可能等待自然更新",
                )
            }
        }

        installFeature("status_bar") {
            SystemUiMembers.resolveStatusBar(classLoader, diagnostics)?.let { members ->
                StatusBarHooks(
                    hooks = hooks,
                    diagnostics = diagnostics,
                    configuration = configuration,
                ).install(members)
            }
        }

        installFeature("panel") {
            SystemUiMembers.resolvePanel(classLoader, diagnostics)?.let { members ->
                NotificationPanelHooks(
                    hooks = hooks,
                    diagnostics = diagnostics,
                    configuration = configuration,
                    members = members,
                ).install()
            }
        }

        installFeature("aod") {
            SystemUiMembers.resolveAod(classLoader, diagnostics)?.let { members ->
                AodNotificationHooks(
                    hooks = hooks,
                    diagnostics = diagnostics,
                    configuration = configuration,
                    members = members,
                ).install()
            }
        }

        installFeature("lockscreen_capsule") {
            SystemUiMembers.resolveLockScreenCapsule(classLoader, diagnostics)?.let { members ->
                LockScreenCapsuleHooks(
                    hooks = hooks,
                    diagnostics = diagnostics,
                    configuration = configuration,
                    members = members,
                ).install()
            }
        }

        installFeature("fullscreen_banner") {
            SystemUiMembers.resolveFullScreenBanner(classLoader, diagnostics)?.let { members ->
                FullScreenBannerHooks(
                    hooks = hooks,
                    diagnostics = diagnostics,
                    configuration = configuration,
                    members = members,
                ).install()
            }
        }
    }

    private inline fun runPublishedTask(
        snapshot: RuntimeSnapshot,
        feature: String,
        block: () -> Unit,
    ) {
        try {
            block()
        } catch (exception: Exception) {
            diagnostics.runtimeFailure(
                scope = "config:on_published:$feature",
                message = "配置快照已发布，但关联功能刷新失败：$feature",
                cause = exception,
                revision = snapshot.revision,
            )
        }
    }

    private inline fun installFeature(feature: String, block: () -> Unit) {
        try {
            block()
        } catch (exception: Exception) {
            diagnostics.installationFailure(
                scope = "systemui:feature:$feature",
                message = "SystemUI 功能初始化失败，已独立降级：$feature",
                cause = exception,
            )
        }
    }
}
