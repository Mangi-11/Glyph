package io.github.mangi.glyph.update

import io.github.mangi.glyph.BuildConfig
import io.github.mangi.glyph.core.ModuleInfo
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object UpdateChecker {

    data class Result(
        val hasUpdate: Boolean,
        val message: String,
        val releaseUrl: String = ModuleInfo.RELEASES_PAGE,
    )

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .callTimeout(18, TimeUnit.SECONDS)
        .build()

    fun check(): Result = try {
        val request = Request.Builder()
            .url(ModuleInfo.RELEASES_API)
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .header("User-Agent", "Glyph/${BuildConfig.VERSION_NAME}")
            .get()
            .build()
        httpClient.newCall(request).execute().use { response ->
            val body = response.body.string().trim()
            when (response.code) {
                404 -> Result(
                    hasUpdate = false,
                    message = "尚未发布 Release，可前往 GitHub 查看仓库。",
                    releaseUrl = ModuleInfo.PROJECT_URL,
                )
                in 200..299 -> parseRelease(body)
                else -> Result(
                    hasUpdate = false,
                    message = "检查失败（HTTP ${response.code}）",
                )
            }
        }
    } catch (exception: Exception) {
        Result(
            hasUpdate = false,
            message = "检查更新失败：${exception.localizedMessage ?: exception.javaClass.simpleName}",
        )
    }

    private fun parseRelease(body: String): Result {
        val json = JSONObject(body)
        val tag = json.optString("tag_name").orEmpty()
        val htmlUrl = json.optString("html_url").ifBlank { ModuleInfo.RELEASES_PAGE }
        val notes = json.optString("body").orEmpty().trim().take(800)
        val latest = normalizeVersion(tag)
        val current = normalizeVersion(BuildConfig.VERSION_NAME)
        if (latest.isEmpty()) {
            return Result(hasUpdate = false, message = "无法解析最新版本号", releaseUrl = htmlUrl)
        }
        return if (compareVersion(latest, current) > 0) {
            Result(
                hasUpdate = true,
                message = buildString {
                    append("发现新版本：$tag\n")
                    append("当前版本：${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                    if (notes.isNotEmpty()) {
                        append("\n\n")
                        append(notes)
                    }
                },
                releaseUrl = htmlUrl,
            )
        } else {
            Result(
                hasUpdate = false,
                message = "已是最新版本（${BuildConfig.VERSION_NAME}）\n最新 Release：$tag",
                releaseUrl = htmlUrl,
            )
        }
    }

    private fun normalizeVersion(raw: String): String =
        raw.trim().removePrefix("v").removePrefix("V").substringBefore("-").trim()

    private fun compareVersion(left: String, right: String): Int {
        val leftParts = left.split('.').map { it.toIntOrNull() ?: 0 }
        val rightParts = right.split('.').map { it.toIntOrNull() ?: 0 }
        val size = maxOf(leftParts.size, rightParts.size)
        for (index in 0 until size) {
            val delta = leftParts.getOrElse(index) { 0 } - rightParts.getOrElse(index) { 0 }
            if (delta != 0) return delta
        }
        return 0
    }
}
