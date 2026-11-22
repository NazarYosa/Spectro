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

    val webCookies = withContext(Dispatchers.Main) {
        val cm = CookieManager.getInstance()
        val musicCookies = cm.getCookie(MusicOrigin).orEmpty()
        val ytCookies = cm.getCookie("https://www.youtube.com").orEmpty()
        val googleCookies = cm.getCookie("https://accounts.google.com").orEmpty()
        listOf(musicCookies, ytCookies, googleCookies).filter { it.isNotBlank() }.joinToString("; ")
    }

    val hasAuthCookies = (webCookies.isNotBlank() && (webCookies.contains("SAPISID=") || webCookies.contains("__Secure-3PAPISID="))) ||
        (storedCookies?.let { it.contains("SAPISID=") || it.contains("__Secure-3PAPISID=") } == true)

    val isGoogle = entry == Entry.Google || hasAuthCookies

    val cookies = if (isGoogle) {
        if (webCookies.isNotBlank() && (webCookies.contains("SAPISID=") || webCookies.contains("__Secure-3PAPISID="))) {
            webCookies
        } else {
            storedCookies ?: webCookies.takeIf { it.isNotBlank() }
        }
    } else {
        null
    }

    // Ensure EntryStore is kept in sync if authentication cookies are active
    if (hasAuthCookies && entry != Entry.Google) {
        entryStore.save(Entry.Google)
    }

    return YouTubeSession(cookies = cookies, visitorData = VisitorStore(context).get(locale), locale = locale)
}
