package com.spectro.app.features.settings.ui

// Android file sizes and widgets
import android.text.format.Formatter
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
// UI styling and utilities
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
// Coroutines and image loading
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import coil3.SingletonImageLoader
// Project resources
import com.spectro.app.R
import com.spectro.app.core.innertube.OfflineCache
import com.spectro.app.features.settings.ui.components.SettingsActionRow
import com.spectro.app.features.settings.ui.components.SettingsDivider
import com.spectro.app.features.settings.ui.components.SettingsGroup
import com.spectro.app.features.settings.ui.components.SettingsInfoRow
import com.spectro.app.features.settings.ui.components.SettingsPage
import com.spectro.app.core.designsystem.components.Toasts

// The storage, how much the saved pages and the cache take, and the ways to free it
@Composable
fun StorageSettingsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()

    // The sizes are read again whenever something is freed, they are read away from the screen because they walk through files
    var refresh by remember { mutableIntStateOf(0) }
    var pagesBytes by remember { mutableStateOf(0L) }
    var cacheBytes by remember { mutableStateOf(0L) }
    LaunchedEffect(refresh) {
        withContext(Dispatchers.IO) {
            pagesBytes = OfflineCache.sizeBytes()
            cacheBytes = SingletonImageLoader.get(context).diskCache?.size ?: 0L
        }
    }
    fun size(bytes: Long) = Formatter.formatShortFileSize(context, bytes)

    SettingsPage(title = stringResource(R.string.settings_storage), onBack = onBack, modifier = modifier) {
        SettingsGroup(title = stringResource(R.string.settings_group_in_use)) {
            SettingsInfoRow(title = stringResource(R.string.settings_storage_pages), value = size(pagesBytes))
            SettingsDivider()
            SettingsInfoRow(title = stringResource(R.string.settings_storage_cache), value = size(cacheBytes))
        }

        SettingsGroup(title = stringResource(R.string.settings_group_clean)) {
            SettingsActionRow(
                title = stringResource(R.string.settings_storage_clear_pages),
                subtitle = stringResource(R.string.settings_storage_clear_pages_subtitle),
                onClick = {
                    OfflineCache.clear()
                    refresh++
                    Toasts.show(resources.getString(R.string.settings_storage_cleared))
                }
            )
            SettingsDivider()
            SettingsActionRow(
                title = stringResource(R.string.settings_storage_clear_cache),
                subtitle = stringResource(R.string.settings_storage_clear_cache_subtitle),
                onClick = {
                    scope.launch(Dispatchers.IO) { SingletonImageLoader.get(context).diskCache?.clear() }
                    refresh++
                    Toasts.show(resources.getString(R.string.settings_storage_cleared))
                }
            )
        }
    }
}
