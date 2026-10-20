package io.github.mangi.glyph.hook.systemui

import android.graphics.drawable.Icon
import java.util.concurrent.ConcurrentHashMap

/**
 * Shares successful status-bar Icon replacements with AOD / lock-screen capsule
 * pipelines that never call IconManager.getIconDescriptor.
 */
internal object StatusBarIconReplacementCache {
    data class Entry(
        val icon: Icon,
        val isColorable: Boolean,
    )

    private val byKey = ConcurrentHashMap<String, Entry>()
    private val byPackage = ConcurrentHashMap<String, Entry>()

    fun put(notificationKey: String, packageName: String, icon: Icon, isColorable: Boolean) {
        val entry = Entry(icon, isColorable)
        byKey[notificationKey] = entry
        byPackage[packageName] = entry
    }

    fun iconFor(notificationKey: String?, packageName: String?): Icon? =
        lookup(notificationKey, packageName)?.icon

    fun lookup(notificationKey: String?, packageName: String?): Entry? {
        if (notificationKey != null) {
            byKey[notificationKey]?.let { return it }
        }
        if (packageName != null) {
            return byPackage[packageName]
        }
        return null
    }

    fun clear() {
        byKey.clear()
        byPackage.clear()
    }
}
