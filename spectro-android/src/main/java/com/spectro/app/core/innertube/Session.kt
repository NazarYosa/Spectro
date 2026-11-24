package com.spectro.app.core.innertube

// Android context and web storage
import android.content.Context
import android.webkit.CookieManager
// Coroutines
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
// Project resources
import com.spectro.app.features.auth.data.AccountStore
import com.spectro.app.features.auth.data.Entry
import com.spectro.app.features.auth.data.EntryStore

// Who is asking YouTube Music, the language of the device, the visitor kept on it and the account when the user signed in with Google
suspend fun currentSession(context: Context): YouTubeSession {
    val locale = deviceLocale()
    val entryStore = EntryStore(context)
    val accountStore = AccountStore(context)
    val entry = entryStore.entry.first()
    val storedCookies = accountStore.cookies.first()

    val musicCookies = withContext(Dispatchers.Main) {
        val cm = CookieManager.getInstance()
        cm.getCookie(MusicOrigin).orEmpty()
    }

    val isAuth = { s: String? ->
        !s.isNullOrBlank() && (s.contains("SAPISID=") || s.contains("__Secure-3PAPISID="))
    }

    val hasAuthCookies = isAuth(musicCookies) || isAuth(storedCookies)
    val isGoogle = entry == Entry.Google || hasAuthCookies

    // Prefer clean MusicOrigin cookies directly; fall back to storedCookies in AccountStore
    val cookies = if (isGoogle) {
        when {
            isAuth(musicCookies) -> musicCookies
            isAuth(storedCookies) -> storedCookies
            else -> musicCookies.ifBlank { storedCookies }
        }
    } else {
        null
    }

    // Keep stored cookies synced if musicCookies is freshly active
    if (isAuth(musicCookies) && musicCookies != storedCookies) {
        accountStore.saveCookies(musicCookies)
    }

    // Ensure EntryStore is kept in sync if authentication cookies are active
    if (hasAuthCookies && entry != Entry.Google) {
        entryStore.save(Entry.Google)
    }

    return YouTubeSession(cookies = cookies, visitorData = VisitorStore(context).get(locale), locale = locale)
}
