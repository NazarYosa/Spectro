package com.spectro.app.core.navigation

// Android WebKit and Compose animation
import android.webkit.CookieManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
// Material 3 components
import androidx.compose.material3.MaterialTheme
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
// UI utilities
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
// Coroutines and reactive flows
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
// Project resources
import com.spectro.app.core.designsystem.theme.ThemeMode
import com.spectro.app.core.designsystem.theme.SpectroMotion
import com.spectro.app.core.designsystem.theme.SpectroTheme
import com.spectro.app.core.innertube.MusicOrigin
import com.spectro.app.features.auth.data.AccountClient
import com.spectro.app.features.auth.data.AccountSession
import com.spectro.app.features.auth.data.AccountStore
import com.spectro.app.features.auth.data.Entry
import com.spectro.app.features.auth.data.EntryStore
import com.spectro.app.features.auth.data.ProfilePhotoStore
import com.spectro.app.features.auth.ui.GoogleLoginScreen
import com.spectro.app.features.auth.ui.WelcomeScreen
import com.spectro.app.features.profile.ui.LocalProfile
import com.spectro.app.features.profile.ui.Profile

// Where the user is, before the app opens
private enum class Stage { Welcome, GoogleLogin, Main }

// Welcome with the ways in, then the app, with a short fade between them, straight to the app when the user is already in
@Composable
fun SpectroApp(startTab: MainTab = MainTab.HOME) {
    val context = LocalContext.current
    val entryStore = remember { EntryStore(context) }
    val accountStore = remember { AccountStore(context) }
    val scope = rememberCoroutineScope()

    // Empty until the saved entry is read, so the welcome does not flash for a user who is already in
    var stage by rememberSaveable { mutableStateOf<Stage?>(null) }

    // True while the welcome is open from inside the app, so back returns to it
    var fromApp by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(stage == null) {
        if (stage == null) {
            if (entryStore.entry.first() == null) {
                entryStore.save(Entry.Guest)
            }
            stage = Stage.Main
        }
    }

    // Counts the times the photo on the device changed, so it is read again
    var photoVersion by remember { mutableIntStateOf(0) }

    val photoChanges by ProfilePhotoStore.changes.collectAsState()

    // Photo of the account, read each time the app opens, the user comes in or the photo changes
    val profilePhoto by produceState<ImageBitmap?>(initialValue = null, stage == Stage.Main, photoVersion, photoChanges) {
        value = if (stage == Stage.Main) ProfilePhotoStore.load(context) else null
    }

    // Name and user name of the account, and how the user came in
    val accountDetails by accountStore.details.collectAsState(initial = null)
    val entry by entryStore.entry.collectAsState(initial = null)
    val profile = Profile(
        photo = profilePhoto,
        name = accountDetails?.name,
        handle = accountDetails?.handle,
        isSignedIn = entry == Entry.Google
    )

    // Each time the app opens, a user signed in with Google gets the account checked, a failed check keeps what is on the device
    LaunchedEffect(stage == Stage.Main) {
        if (stage == Stage.Main && entryStore.entry.first() == Entry.Google) {
            val cookies = CookieManager.getInstance().getCookie(MusicOrigin)
            val account = cookies?.takeIf { it.isNotBlank() }?.let { AccountClient.fetchAccount(it).getOrNull() }
            if (account != null && AccountSession.apply(context, account).getOrDefault(false)) photoVersion++
        }
    }

    // Back on the welcome opened from the profile goes back to the app as a guest
    BackHandler(enabled = stage == Stage.Welcome && fromApp) {
        scope.launch { entryStore.save(Entry.Guest) }
        fromApp = false
        stage = Stage.Main
    }

    CompositionLocalProvider(LocalProfile provides profile) {
        Crossfade(
            targetState = stage,
            animationSpec = tween(SpectroMotion.ScreenFadeMillis),
            label = "AppStage"
        ) { current ->
            when (current) {
                null -> Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                )

                // The welcome is always dark, over a photo
                Stage.Welcome -> SpectroTheme(themeMode = ThemeMode.DARK) {
                    WelcomeScreen(
                        onGoogleClick = { stage = Stage.GoogleLogin },
                        onSkipClick = {
                            scope.launch { entryStore.save(Entry.Guest) }
                            fromApp = false
                            stage = Stage.Main
                        }
                    )
                }

                Stage.GoogleLogin -> GoogleLoginScreen(
                    onSuccess = {
                        scope.launch { entryStore.save(Entry.Google) }
                        fromApp = false
                        stage = Stage.Main
                    },
                    onBack = { stage = Stage.Main }
                )

                Stage.Main -> MainScaffold(
                    startTab = startTab,
                    onSignOut = {
                        scope.launch {
                            AccountSession.signOut(context)
                            stage = Stage.Welcome
                        }
                    },
                    onSignIn = {
                        // Leaving the guest choice, so the welcome shows again if the app is closed here
                        scope.launch { entryStore.clear() }
                        fromApp = true
                        stage = Stage.Welcome
                    }
                )
            }
        }
    }
}
