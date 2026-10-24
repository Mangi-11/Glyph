# Glyph

Glyph 是 fork 自 [fankes/ColorOSNotifyIcon](https://github.com/fankes/ColorOSNotifyIcon) 的非官方个人分支，为 ColorOS 优化通知图标，可适配原生通知图标规范，或让通知图标跟随桌面当前主题。本分支由 [Mangi-11](https://github.com/Mangi-11) 独立维护和发布。

Hook 层采用原作者 [fankes](https://github.com/fankes) 开发的 [YukiHook](https://github.com/HighCapable/YukiHook)，搭配 [KavaRef](https://github.com/HighCapable/KavaRef) 进行反射查找。YukiHook 提供简洁的 Kotlin Hook API、自动生成的模块入口和统一的日志支持，减少重复代码，便于维护与扩展。

当前使用 YukiHook 1.5.0 / KavaRef 1.1.1，采用 libxposed API 102 底座，并使用 [Miuix](https://github.com/compose-miuix-ui/miuix) 重写了 UI。应用包名为 `io.github.mangi.glyph`。

## 功能

- 状态栏通知图标替换
- 息屏（AOD）时钟通知图标替换
- ColorOS 16 锁屏岛（底部通知胶囊）图标替换
- 通知中心小图标替换（可关）
- 图标来源可选规则图标或桌面主题，支持通知图标跟随桌面当前主题 / 自定义图标
- Oplus Push 系统推送特判（可关）
- 未适配应用使用通用圆形通知图标作为占位符（可关）
- 本地规则管理，支持按应用启用或全部替换
- 支持为应用手动指定规则库图标，优先于包名匹配
- 手动同步远程规则，不做后台自动同步
- 配置写入 Xposed 框架侧，通知 SystemUI 刷新
- 桌面图标可隐藏，隐藏后仍可从 LSPosed 模块列表打开

## 功能设置

| 设置 | 说明 |
| --- | --- |
| 启用图标增强 | 总开关。关闭后不再使用规则或主题图标，只保留符合通知规范的原始 `smallIcon` 保护 |
| 图标来源 | 在规则图标和桌面主题之间二选一。桌面主题模式下，通知图标跟随桌面当前主题或自定义图标 |
| 通知中心图标替换 | 控制通知面板内的小图标。关闭后通知中心保持 ColorOS 默认行为，不影响状态栏 |
| 系统推送特判 | 规则图标模式下，Oplus Push 通知是否优先使用目标应用的已启用规则。关闭后保持系统默认 |
| 未适配占位符 | 规则图标模式下，未命中规则且原始图标不是可着色的通知遮罩时，使用通用圆形通知图标替代 |
| 隐藏桌面图标 | 隐藏 Glyph 的桌面入口，不影响从 LSPosed 模块列表打开 |

规则图标模式下，单条规则里的两个选项：

- **启用替换**：允许该应用使用规则图标
- **全部替换**：忽略应用自带且符合规范的 `smallIcon`，始终使用规则图标

## 图标来源

状态栏、息屏时钟、锁屏岛、通知中心普通图标、Oplus 聚合摘要图标共享同一套图标来源逻辑。

- **规则图标**：使用模块规则库图标，支持手动指定、未适配占位符和单应用强制替换。
- **桌面主题**：读取桌面当前主题或自定义图标。读取失败时回退系统原始通知图标，不回退规则图标，避免两套来源混用。

## 图标决策逻辑

规则图标模式下，状态栏和通知中心用同一套逻辑，不会无条件覆盖系统结果：

1. Oplus Push 通知 + 系统推送特判已关闭 → 保持系统默认
2. Oplus Push 通知 + 系统推送特判已开启 + 命中已启用规则 → 使用规则图标
3. 命中已启用规则 + 全部替换已开启 → 使用规则图标
4. 命中已启用规则 + 原始 `smallIcon` 不是可着色的通知遮罩 → 使用规则图标
5. 未命中规则 + 未适配占位符已开启 + 原始 `smallIcon` 不是可着色的通知遮罩 → 使用通用圆形通知图标占位
6. 原始 `smallIcon` 是可着色的通知遮罩 → 恢复原始 `smallIcon`
7. 以上均不满足 → 保持 ColorOS 当前结果

「命中规则」按通知的包名精确查找。用户可在规则管理页为某个应用手动指定规则库中的另一枚图标，手动指定优先于包名匹配；图标只取被借用条目在规则库中的原始定义，不传递该条目自己的手动指定，避免循环。未进入规则库的已安装应用也可以添加一条手动指定，清除后回退包名匹配（未适配应用则从列表中移除）。
启用替换、全部替换、适配系统推送仍按**当前应用**自己的开关判断，手动指定只改图标。切换到「桌面主题」后不会使用手动指定。

兼容性判断同时验证通知遮罩的 Alpha 几何和单色前景合同：图标需要有透明背景，
且可见内容应为灰阶或单一前景色。多彩图、彩色与中性色混合、致密圆角底板、
`AdaptiveIconDrawable`、全透明图和全不透明图均不会被当作通知遮罩强制着色；
命中规则或占位符时使用对应替代图标，否则保留 ColorOS 当前结果。无法安全读取像素的
硬件位图属于未知结果，直接交回 SystemUI 判定。RGB 不参与最终渲染，但会用于区分
可着色的通知资源与桌面应用图标。
Oplus Push 代表目标应用生成的位图不等同于应用原生 `smallIcon`；命中已启用规则时，
不会因为该位图带有透明圆角而跳过规则。

恢复的原生 `smallIcon`、规则图标和占位符继续由 SystemUI 统一着色；桌面主题来源是
全彩应用图标，保持原色，不套用通知遮罩的灰度着色。
重要会话的联系人头像保留 SystemUI 原生路径，不替换成通知遮罩。

通知中心额外跳过媒体通知，不破坏系统媒体样式。

## 安装

1. 从 [GitHub Releases](https://github.com/Mangi-11/Glyph/releases/latest) 下载 APK
2. 在 LSPosed 中启用模块
3. 勾选作用域：系统框架 `system`、系统界面 `com.android.systemui`
4. 打开 Glyph（隐藏桌面图标后可从 LSPosed 模块列表打开），同步规则
5. 按需调整图标来源、开关和单个应用规则
6. 点击 **重启 SystemUI** 使配置生效

首次安装或更新了涉及 `system_server` Hook 的版本，建议完整重启一次系统。

## 适配说明

仅适配 ColorOS 16 / realme UI 7.0 + LSPosed，不兼容旧版系统、旧版 Xposed 或其他框架。

只服务最新系统版本，去掉历史兼容包袱，保持干净优雅。

## 规则来源

沿用 [Android Notification Icon Project (ANIP)](https://github.com/BetterAndroid/android-notification-icon-project) 规则仓库：

- [Android Notification Icon Project](https://github.com/BetterAndroid/android-notification-icon-project)
- [ColorOS 规则清单](https://raw.githubusercontent.com/BetterAndroid/android-notification-icon-project/main/icons/system/coloros/manifest.json)
- [游戏规则清单](https://raw.githubusercontent.com/BetterAndroid/android-notification-icon-project/main/icons/game/manifest.json)
- [APP 规则清单](https://raw.githubusercontent.com/BetterAndroid/android-notification-icon-project/main/icons/app/manifest.json)

同步时按 ColorOS → 游戏 → APP 合并。同一包名出现多次的，以后者为准。

## 与原版的区别

本分支沿用原项目的 YukiHook + KavaRef 技术栈，保留原始协议、版权声明和贡献者致谢。Hook 入口与作用域配置由 KSP 自动生成，配置和规则文件通过 libxposed 远程同步。

主要改动：

- 移除了旧框架兼容层
- 用 [Miuix](https://github.com/compose-miuix-ui/miuix) 重写了 App UI（首页 / 规则 / 关于三栏，液态玻璃底栏）
- 关于页支持打开本项目与 GitHub Releases 检查更新
- 功能收敛到通知图标增强，去除其余杂项功能
- 支持通知图标跟随桌面主题或自定义图标
- 补齐 ColorOS 经典时钟息屏通知图标（`LockScreenNotificationIconData` → AodPlugin，以及旧版 `NotificationLayout`）
- 补齐 ColorOS 16 锁屏岛底部通知胶囊图标（`CapsuleNotificationDataController`）。规则图标在锁屏岛染白轮廓，桌面主题保持彩色，避免多条聚合被白色遮罩盖成色块；白色着色不作用于下拉通知与锁屏堆叠
- 规则管理支持为单个应用手动指定规则库图标，优先于包名匹配
- 不做后台自动同步

原版项目以原作者维护计划为准。

## 注意事项

1. 本软件免费、兴趣驱动，仅供学习交流。如果你是付费获得的，说明被骗了。
2. 本软件采用 **AGPL 3.0** 许可证。分发或修改时必须遵守条款并提供源代码。
3. 请保留原始版权声明和许可证信息，不要冒用原作者名义。

## 隐私政策

- [PRIVACY](PRIVACY.md)

## 许可证

- [AGPL-3.0](https://www.gnu.org/licenses/agpl-3.0.html)

原始项目版权归 Fankes Studio(qzmmcn@163.com) 所有。本分支改动同样按 AGPL-3.0 发布。

## 致谢

感谢原作者 [fankes](https://github.com/fankes) 的开源基础，以及各位图标规则维护者的持续付出，得以告别丑陋的默认图标，带来愉悦。

- [ColorOSNotifyIcon](https://github.com/fankes/ColorOSNotifyIcon)：本分支的原始项目。
- [YukiHook](https://github.com/HighCapable/YukiHook)：同样由原作者 fankes 开发，为本分支提供 Hook 开发支持。
- [KavaRef](https://github.com/HighCapable/KavaRef)：提供 Kotlin 反射查找能力。
- [Miuix](https://github.com/compose-miuix-ui/miuix)：提供应用 UI 组件。
- [ANIP](https://github.com/BetterAndroid/android-notification-icon-project)：提供通知图标规则，感谢规则维护者的持续更新。
- 通用占位符采用 Google Material Icons 的 [`circle_notifications`](https://github.com/google/material-design-icons)，按 [Apache-2.0](third_party/material-design-icons/LICENSE) 许可证使用。
