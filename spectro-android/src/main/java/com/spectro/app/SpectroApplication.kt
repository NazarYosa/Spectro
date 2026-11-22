package com.spectro.app

// Android application
import android.app.Application
import com.spectro.app.core.network.SpectroProxy
import com.spectro.app.core.storage.StorageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

// The application of the app, it starts before the screens, the player and the background work, so all of them find the proxy ready
class SpectroApplication : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        SpectroProxy.install(this)

        // Automatically eliminate leftover WebView cache, GPUCache & SQLite WAL files
        appScope.launch {
            StorageManager.trimStorage(this@SpectroApplication)
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        appScope.launch {
            StorageManager.trimStorage(this@SpectroApplication)
        }
    }
}
