package io.github.mangi.glyph.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleCatalogResolveTest {

    @Test
    fun manualIconSourceTakesPriorityOverPackageMatch() {
        val catalog = catalog()
        val resolved = catalog.resolve(
            RuleOverrides(iconSource = mapOf("com.example.a" to "com.example.b"))
        )
        val rule = resolved.byPackage.getValue("com.example.a")

        assertEquals("App A", rule.appName)
        assertEquals("com.example.b", rule.iconSourcePackage)
        assertSame(catalog.byPackage.getValue("com.example.b"), rule.sourcedFrom)
        assertSame(assetB, rule.iconAsset)
        assertEquals(COLOR_B, rule.iconColor)
        assertTrue(rule.isLibraryEntry)
    }

    @Test
    fun selfSourceIsTreatedAsCleared() {
        val catalog = catalog()
        val resolved = catalog.resolve(
            RuleOverrides(iconSource = mapOf("com.example.a" to "com.example.a"))
        )
        val rule = resolved.byPackage.getValue("com.example.a")

        assertNull(rule.iconSourcePackage)
        assertNull(rule.sourcedFrom)
        assertSame(assetA, rule.iconAsset)
    }

    @Test
    fun missingSourceKeepsRequestedPackageAndFallsBackToCatalogIcon() {
        val catalog = catalog()
        val resolved = catalog.resolve(
            RuleOverrides(iconSource = mapOf("com.example.a" to "com.example.missing"))
        )
        val rule = resolved.byPackage.getValue("com.example.a")

        assertEquals("com.example.missing", rule.iconSourcePackage)
        assertNull(rule.sourcedFrom)
        assertSame(assetA, rule.iconAsset)
    }

    @Test
    fun iconSourceDoesNotFollowAnotherOverride() {
        val catalog = catalog()
        val resolved = catalog.resolve(
            RuleOverrides(
                iconSource = mapOf(
                    "com.example.a" to "com.example.b",
                    "com.example.b" to "com.example.c",
                )
            )
        )

        assertSame(assetB, resolved.byPackage.getValue("com.example.a").iconAsset)
        assertSame(assetC, resolved.byPackage.getValue("com.example.b").iconAsset)
    }

    @Test
    fun targetEnablementIsIndependentOfSource() {
        val catalog = catalog()
        val resolved = catalog.resolve(
            RuleOverrides(
                enabled = mapOf("com.example.a" to false, "com.example.b" to true),
                enabledAll = mapOf("com.example.a" to true),
                iconSource = mapOf("com.example.a" to "com.example.b"),
            )
        )
        val rule = resolved.byPackage.getValue("com.example.a")

        assertFalse(rule.isEnabled)
        assertTrue(rule.isEnabledAll)
        assertSame(assetB, rule.iconAsset)
    }

    @Test
    fun unadaptedPackageBecomesSyntheticRule() {
        val catalog = catalog()
        val resolved = catalog.resolve(
            RuleOverrides(
                iconSource = mapOf("com.example.custom" to "com.example.b"),
                customNames = mapOf("com.example.custom" to "分身 B"),
                enabledAll = mapOf("com.example.custom" to true),
            )
        )
        val rule = resolved.byPackage.getValue("com.example.custom")

        assertEquals("分身 B", rule.appName)
        assertEquals("com.example.custom", rule.packageName)
        assertFalse(rule.isLibraryEntry)
        assertTrue(rule.isEnabled)
        assertTrue(rule.isEnabledAll)
        assertSame(assetB, rule.iconAsset)
        assertEquals(COLOR_B, rule.iconColor)
        assertEquals("com.example.b", rule.iconSourcePackage)
    }

    @Test
    fun syntheticRuleWithoutCustomNameUsesPackageName() {
        val catalog = catalog()
        val resolved = catalog.resolve(
            RuleOverrides(iconSource = mapOf("com.example.custom" to "com.example.a"))
        )

        assertEquals("com.example.custom", resolved.byPackage.getValue("com.example.custom").appName)
    }

    @Test
    fun syntheticRuleIsDroppedWhenSourceIsMissing() {
        val catalog = catalog()
        val resolved = catalog.resolve(
            RuleOverrides(iconSource = mapOf("com.example.custom" to "com.example.missing"))
        )

        assertFalse(resolved.byPackage.containsKey("com.example.custom"))
        assertEquals(catalog.size, resolved.size)
    }

    private fun catalog() = RuleCatalogParser.fromInputs(
        inputs = listOf(
            input("com.example.a", "App A", "a", "#111111"),
            input("com.example.b", "App B", "b", "#222222"),
            input("com.example.c", "App C", "c", "#333333"),
        ),
        iconFactory = { encoded ->
            when (encoded) {
                "a" -> assetA
                "b" -> assetB
                else -> assetC
            }
        },
    )

    private fun input(
        packageName: String,
        appName: String,
        iconBase64: String,
        iconColor: String,
    ) = RuleInput(
        appName = appName,
        packageName = packageName,
        iconBase64 = iconBase64,
        iconColor = iconColor,
        contributorName = "Tester",
        enabledByDefault = true,
        enabledAllByDefault = false,
    )

    private companion object {
        val assetA = IconAsset.fromBytesForTest(byteArrayOf(1))
        val assetB = IconAsset.fromBytesForTest(byteArrayOf(2))
        val assetC = IconAsset.fromBytesForTest(byteArrayOf(3))
        val COLOR_B = 0xFF222222.toInt()
    }
}
