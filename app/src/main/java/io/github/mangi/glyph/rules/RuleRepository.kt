package io.github.mangi.glyph.rules

import io.github.mangi.glyph.diagnostics.AppDiagnostics
import io.github.mangi.glyph.diagnostics.DiagnosticEvent
import io.github.mangi.glyph.diagnostics.DiagnosticLevel
import io.github.mangi.glyph.framework.MainThreadCallbacks
import okhttp3.OkHttpClient
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object RuleRepository {

    data class SyncResult(
        val count: Int,
        val updatedAt: Long,
    )

    sealed class SyncFailure(
        val code: String,
        val userMessage: String,
        message: String,
        cause: Exception? = null,
    ) : Exception(message, cause) {
        class Download(source: String, cause: Exception) : SyncFailure(
            code = "download",
            userMessage = "规则下载失败",
            message = "Unable to download $source rule catalog",
            cause = cause,
        )

        class InvalidPayload(cause: Exception) : SyncFailure(
            code = "invalid_payload",
            userMessage = "下载的规则数据无效",
            message = "Downloaded rule catalog is invalid",
            cause = cause,
        )

        class LocalSave(cause: Exception) : SyncFailure(
            code = "local_save",
            userMessage = "规则保存失败",
            message = "Unable to save downloaded rule catalog",
            cause = cause,
        )

        class Scheduling(cause: Exception) : SyncFailure(
            code = "schedule",
            userMessage = "规则同步任务无法调度",
            message = "Unable to schedule rule synchronization",
            cause = cause,
        )
    }

    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "rule-downloader")
    }
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .callTimeout(180, TimeUnit.SECONDS)
        .build()

    fun syncRules(onResult: (Result<SyncResult>) -> Unit) {
        try {
            executor.execute {
                val result = try {
                    Result.success(syncBlocking())
                } catch (exception: Exception) {
                    logFailure(exception)
                    Result.failure(exception)
                }
                deliver(result, onResult)
            }
        } catch (exception: Exception) {
            val failure = SyncFailure.Scheduling(exception)
            logFailure(failure)
            deliver(Result.failure(failure), onResult)
        }
    }

    private fun deliver(result: Result<SyncResult>, callback: (Result<SyncResult>) -> Unit) {
        MainThreadCallbacks.dispatch("rules_sync") { callback(result) }
    }

    private fun syncBlocking(): SyncResult {
        val merged = try {
            AnipRuleSource.sync(httpClient)
        } catch (exception: RuleRepository.SyncFailure) {
            throw exception
        } catch (exception: Exception) {
            throw SyncFailure.InvalidPayload(exception)
        }
        val updatedAt = System.currentTimeMillis()
        val catalog = try {
            RuleStore.updateRules(merged, updatedAt)
        } catch (exception: RuleParseException) {
            throw SyncFailure.InvalidPayload(exception)
        } catch (exception: IllegalArgumentException) {
            throw SyncFailure.InvalidPayload(exception)
        } catch (exception: Exception) {
            throw SyncFailure.LocalSave(exception)
        }
        AppDiagnostics.logger.report(
            level = DiagnosticLevel.Info,
            event = DiagnosticEvent.RulesDownloaded,
            message = "Rule catalogs downloaded and validated",
            attributes = mapOf("rules" to catalog.size),
        )
        return SyncResult(count = catalog.size, updatedAt = updatedAt)
    }

    private fun logFailure(exception: Exception) {
        val event = when (exception) {
            is SyncFailure.InvalidPayload -> DiagnosticEvent.RulesParseFailed
            is SyncFailure.LocalSave -> DiagnosticEvent.RulesSaveFailed
            else -> DiagnosticEvent.RulesDownloadFailed
        }
        AppDiagnostics.logger.report(
            level = DiagnosticLevel.Error,
            event = event,
            message = (exception as? SyncFailure)?.userMessage ?: "规则同步失败",
            cause = exception,
            attributes = mapOf(
                "failure" to ((exception as? SyncFailure)?.code ?: "unexpected"),
            ),
        )
    }

}
