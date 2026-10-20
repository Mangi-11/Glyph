package io.github.mangi.glyph.core

object SystemPackages {
    const val SYSTEM_SCOPE = "system"
    const val SYSTEM_UI = "com.android.systemui"
}

object ModuleInfo {
    const val LOG_TAG = "Glyph"
    const val PROJECT_URL = "https://github.com/Mangi-11/Glyph"
    const val RELEASES_PAGE = "$PROJECT_URL/releases"
    const val RELEASES_API = "https://api.github.com/repos/Mangi-11/Glyph/releases/latest"
    const val ANIP_REPO = "BetterAndroid/android-notification-icon-project"
    const val ANIP_BRANCH = "main"
    const val ANIP_RAW_BASE = "https://raw.githubusercontent.com/$ANIP_REPO/$ANIP_BRANCH"
    const val ANIP_CDN_BASE = "https://cdn.jsdelivr.net/gh/$ANIP_REPO@$ANIP_BRANCH"
    const val ANIP_PROJECT_URL = "https://github.com/$ANIP_REPO"
    const val ANIP_APP_MANIFEST_URL = "$ANIP_RAW_BASE/icons/app/manifest.json"
    const val ANIP_GAME_MANIFEST_URL = "$ANIP_RAW_BASE/icons/game/manifest.json"
    const val ANIP_COLOROS_MANIFEST_URL = "$ANIP_RAW_BASE/icons/system/coloros/manifest.json"
    val ANIP_APP_MANIFEST_URLS = listOf(
        ANIP_APP_MANIFEST_URL,
        "$ANIP_CDN_BASE/icons/app/manifest.json",
    )
    val ANIP_GAME_MANIFEST_URLS = listOf(
        ANIP_GAME_MANIFEST_URL,
        "$ANIP_CDN_BASE/icons/game/manifest.json",
    )
    val ANIP_COLOROS_MANIFEST_URLS = listOf(
        ANIP_COLOROS_MANIFEST_URL,
        "$ANIP_CDN_BASE/icons/system/coloros/manifest.json",
    )
}
