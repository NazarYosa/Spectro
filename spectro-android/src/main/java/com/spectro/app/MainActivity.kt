package com.spectro.app

// Android activity components
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.CachePolicy
import com.spectro.app.core.designsystem.theme.SpectroTheme
import com.spectro.app.core.innertube.OfflineCache
import com.spectro.app.core.language.AppLanguage
import com.spectro.app.core.navigation.LinkOpener
import com.spectro.app.core.navigation.MainTab
import com.spectro.app.core.navigation.SpectroApp
import com.spectro.app.core.network.SpectroProxy
import com.spectro.app.core.playback.StreamResolver
import com.spectro.app.features.settings.data.ContentPrefs
import com.spectro.app.features.settings.data.PrivacyPrefs
import com.spectro.app.features.settings.data.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

// Wait after the start before the playback engine warms up, so the Home loads first
private const val PrewarmDelayMs = 2500L

// Single activity of the app, immersive with the content under the cutout
class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguage.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.show(WindowInsetsCompat.Type.systemBars())
        warmUpPlayback(isFirstStart = savedInstanceState == null)

        if (savedInstanceState == null) LinkOpener.offer(intent)

        val store = SettingsStore(this)
        val firstTheme = runBlocking { store.themeMode.first() }

        val firstTab = runBlocking { store.defaultTab.first() }.let { name ->
            MainTab.entries.firstOrNull { it.name == name } ?: MainTab.HOME
        }

        lifecycleScope.launch {
            store.privacy.map { it.blockScreenshots }.distinctUntilChanged().collect { isBlocked ->
                if (isBlocked) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }

        // Pure online image loading: memory cache only, NO disk caching to avoid file bloat
        SingletonImageLoader.setSafe { context ->
            ImageLoader.Builder(context)
                .diskCachePolicy(CachePolicy.DISABLED)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .components {
                    add(OkHttpNetworkFetcherFactory(callFactory = { SpectroProxy.client }))
                }
                .build()
        }

        setContent {
            val themeMode by store.themeMode.collectAsState(initial = firstTheme)

            SpectroTheme(themeMode = themeMode) {
                SpectroApp(startTab = firstTab)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        lifecycleScope.launch(Dispatchers.IO) {
            com.spectro.app.core.storage.StorageManager.trimStorage(applicationContext)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        LinkOpener.offer(intent)
    }

    private fun warmUpPlayback(isFirstStart: Boolean) {
        StreamResolver.initialize(this)
        OfflineCache.initialize(this)
        ContentPrefs.follow(this)
        PrivacyPrefs.follow(this)
        if (!isFirstStart) return

        lifecycleScope.launch(Dispatchers.IO) {
            delay(PrewarmDelayMs)
            runCatching { StreamResolver.prewarm() }
        }
    }
}
