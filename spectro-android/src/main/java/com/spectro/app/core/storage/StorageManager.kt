package com.spectro.app.core.storage

import android.content.Context
import android.webkit.WebStorage
import com.spectro.app.core.history.PlayHistory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * StorageManager ensures Spectro's disk usage remains minimal (~16-21 MB total),
 * actively eliminating hidden Chromium WebView GPU/shader caches, temporary buffers,
 * and SQLite WAL log accumulation.
 */
object StorageManager {

    suspend fun trimStorage(context: Context) = withContext(Dispatchers.IO) {
        runCatching {
            val app = context.applicationContext

            // 1. Purge Chromium WebStorage data
            runCatching {
                WebStorage.getInstance().deleteAllData()
            }

            // 2. Wipe Chromium WebView GPUCache, Code Cache, and shader bloat
            val appDataDir = runCatching { app.applicationInfo.dataDir?.let { File(it) } }.getOrNull()
            val webViewDir = appDataDir?.let { File(it, "app_webview") }
            if (webViewDir != null && webViewDir.exists() && webViewDir.isDirectory) {
                val cacheDirs = listOf(
                    File(webViewDir, "Default/GPUCache"),
                    File(webViewDir, "Default/Code Cache"),
                    File(webViewDir, "Default/Cache"),
                    File(webViewDir, "Default/DawnCache"),
                    File(webViewDir, "Default/Service Worker"),
                    File(webViewDir, "GrShaderCache"),
                    File(webViewDir, "ShaderCache"),
                    File(webViewDir, "Crash Reports")
                )
                for (dir in cacheDirs) {
                    runCatching {
                        if (dir.exists()) dir.deleteRecursively()
                    }
                }
            }

            // 3. Clear standard cache directory
            runCatching {
                app.cacheDir?.listFiles()?.forEach { file ->
                    runCatching { file.deleteRecursively() }
                }
            }

            // 4. Checkpoint SQLite Write-Ahead Log (WAL) to prevent file growth
            runCatching {
                PlayHistory.checkpoint(app)
            }
        }
    }
}
