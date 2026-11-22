package com.spectro.app.core.innertube

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.io.File
import java.security.MessageDigest

// Pure online architecture: no disk caching of responses to keep the app footprint minimal
object OfflineCache {
    @Volatile
    private var directory: File? = null

    @Volatile
    private var connectivity: ConnectivityManager? = null

    fun initialize(context: Context) {
        val app = context.applicationContext
        connectivity = app.getSystemService(ConnectivityManager::class.java)
        // Clean any leftover cache directory from previous versions
        runCatching {
            File(app.filesDir, DirectoryName).deleteRecursively()
        }
    }

    fun isOnline(): Boolean {
        val manager = connectivity ?: return true
        val capabilities = manager.getNetworkCapabilities(manager.activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    fun keyOf(vararg parts: String?): String =
        MessageDigest.getInstance("SHA-256")
            .digest(parts.joinToString("|") { it.orEmpty() }.toByteArray())
            .joinToString("") { "%02x".format(it) }

    fun get(key: String): String? = null

    fun put(key: String, text: String) {
        // No-op: do not write cache to storage
    }

    fun sizeBytes(): Long = 0L

    fun clear() {
        directory?.listFiles()?.forEach { it.delete() }
    }

    private const val DirectoryName = "pages"
}
