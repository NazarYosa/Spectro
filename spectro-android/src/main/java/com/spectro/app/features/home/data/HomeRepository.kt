package com.spectro.app.features.home.data

// Android context
import android.content.Context
// Coroutines and reactive flows
import kotlinx.coroutines.flow.first
// Project resources
import com.spectro.app.core.innertube.InnerTubeClient
import com.spectro.app.core.innertube.currentSession
import com.spectro.app.features.auth.data.AccountStore
import com.spectro.app.features.auth.data.Entry
import com.spectro.app.features.auth.data.EntryStore
import com.spectro.app.features.library.data.LibraryParser

// Pages of YouTube Music that are the Home and the playlists of the account
private const val HomeBrowseId = "FEmusic_home"
private const val AccountPlaylistsBrowseId = "FEmusic_liked_playlists"

// Playlist of the account that holds the episodes saved for later, which is not a playlist of music
private const val EpisodesForLaterId = "SE"

// Asks YouTube Music for the Home, as a guest or with the account of the user when there is one
class HomeRepository(context: Context) {
    private val appContext = context.applicationContext
    private val entryStore = EntryStore(appContext)
    private val accountStore = AccountStore(appContext)

    // The Home, the Home under a filter, or the next page of one of them
    suspend fun load(params: String? = null, continuation: String? = null): Result<HomePage> {
        val session = currentSession(appContext)
        val answer = InnerTubeClient.browse(
            session = session,
            browseId = if (continuation == null) HomeBrowseId else null,
            params = params,
            continuation = continuation
        )

        return answer.mapCatching { response ->
            if (continuation == null) HomeParser.parseFirstPage(response) else HomeParser.parseContinuation(response)
        }
    }

    // Playlists of the account of the user, empty for a guest
    suspend fun accountPlaylists(): Result<List<HomeItem>> {
        val session = currentSession(appContext)
        val isGoogle = entryStore.entry.first() == Entry.Google || !session.cookies.isNullOrBlank() || accountStore.cookies.first() != null
        if (!isGoogle) return Result.success(emptyList())

        return InnerTubeClient.browse(session = session, browseId = AccountPlaylistsBrowseId)
            .mapCatching { response ->
                val parsed = LibraryParser.parse(response).items
                if (parsed.isNotEmpty()) {
                    parsed.filter { it.id != EpisodesForLaterId }
                } else {
                    HomeParser.parseLibraryItems(response).filter { it.id != EpisodesForLaterId }
                }
            }
    }
}
