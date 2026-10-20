package io.github.mangi.glyph.ui.rules

import io.github.mangi.glyph.rules.IconRule
import io.github.mangi.glyph.rules.RuleStore
import java.util.Locale

data class InstalledAppChoice(
    val packageName: String,
    val label: String,
)

data class RuleListState(
    val rules: List<IconRule> = emptyList(),
    val installedPackageNames: Set<String> = emptySet(),
    val installedPackagesKnown: Boolean = false,
    val unadaptedInstalledApps: List<InstalledAppChoice> = emptyList(),
    val query: String = "",
    val config: RuleStore.ModuleConfig = RuleStore.ModuleConfig(),
    val canEditConfig: Boolean = false,
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
) {
    val libraryRules: List<IconRule>
        get() = rules.filter(IconRule::isLibraryEntry)
    val filteredRules: List<IconRule>
        get() {
            val keyword = query.trim().lowercase(Locale.getDefault())
            if (keyword.isBlank()) return rules
            return rules.filter {
                it.appName.lowercase(Locale.getDefault()).contains(keyword) ||
                    it.packageName.lowercase(Locale.getDefault()).contains(keyword) ||
                    it.sourcedFrom?.appName?.lowercase(Locale.getDefault())?.contains(keyword) == true ||
                    it.iconSourcePackage?.lowercase(Locale.getDefault())?.contains(keyword) == true
            }
        }

    val sections: List<RuleSection>
        get() {
            val visibleRules = filteredRules
            if (visibleRules.isEmpty()) return emptyList()
            if (!installedPackagesKnown) {
                return listOf(RuleSection(RuleSectionType.All, visibleRules))
            }
            val (installed, notInstalled) = visibleRules.partition {
                it.packageName in installedPackageNames
            }
            return buildList {
                if (installed.isNotEmpty()) add(RuleSection(RuleSectionType.Installed, installed))
                if (notInstalled.isNotEmpty()) add(RuleSection(RuleSectionType.NotInstalled, notInstalled))
            }
        }

    val installedEnabledRulePackageNames: Set<String>
        get() = rules.asSequence()
            .filter { it.isEnabled && it.packageName in installedPackageNames }
            .mapTo(linkedSetOf()) { it.packageName }

    val installedRulesEnabledAll: Boolean
        get() {
            val eligiblePackages = installedEnabledRulePackageNames
            return eligiblePackages.isNotEmpty() && rules
                .asSequence()
                .filter { it.packageName in eligiblePackages }
                .all(IconRule::isEnabledAll)
        }

}

data class RuleSection(
    val type: RuleSectionType,
    val rules: List<IconRule>,
)

enum class RuleSectionType {
    All,
    Installed,
    NotInstalled,
}
