package com.spectro.app.features.menu

// Android and Compose layouts
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
// Material 3 components
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
// UI utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
// Coroutines and reactive flows
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
// Project resources
import com.spectro.app.R
import com.spectro.app.features.like.data.Likes
import com.spectro.app.core.designsystem.components.LocalSheetClose
import com.spectro.app.core.designsystem.components.SpectroSheet
import com.spectro.app.core.designsystem.icons.SpectroIcons
import com.spectro.app.core.designsystem.theme.SpectroTheme
import com.spectro.app.core.history.PlayHistory
import com.spectro.app.core.innertube.InnerTubeClient
import com.spectro.app.core.innertube.currentSession
import com.spectro.app.core.playback.SleepTimer
import com.spectro.app.core.playback.PlayerConnection
import com.spectro.app.core.playback.RadioQueue
import com.spectro.app.core.designsystem.components.OverlaySheet
import com.spectro.app.features.collection.ui.showCollectionMenu
import com.spectro.app.features.player.ui.components.ArtistPickerSheet
import com.spectro.app.features.home.data.HomeItem
import com.spectro.app.features.home.data.HomeItemKind
import com.spectro.app.features.home.ui.toPlayableTrack
import com.spectro.app.core.device.artistLine
import com.spectro.app.core.device.isDeviceTrack
import com.spectro.app.core.radio.isRadioTrack
import com.spectro.app.core.navigation.ItemNavigator
import com.spectro.app.features.home.ui.components.itemSubtitle
import com.spectro.app.features.like.ui.rememberLikeState
import com.spectro.app.features.player.ui.components.EqualizerSheet
import com.spectro.app.features.player.ui.components.SleepTimerSheet
import com.spectro.app.features.player.ui.components.formatTime
import com.spectro.app.features.playlist.data.PlaylistActions
import com.spectro.app.features.playlist.data.PlaylistChanges
import com.spectro.app.features.playlist.data.PlaylistEvent
import com.spectro.app.features.playlist.data.PlaylistRepository
import com.spectro.app.features.playlist.ui.SaveToPlaylist
import com.spectro.app.features.player.ui.components.TrackCover
import com.spectro.app.features.player.ui.components.shareSong
import com.spectro.app.core.designsystem.components.Toasts

// The playlist of the account a list of songs belongs to, given by its page so the menu of a song can take it out
val LocalEditablePlaylist = compositionLocalOf<String?> { null }

// How often the time left of the sleep timer is read, and the milliseconds in a second
private const val SleepTickMillis = 1000L
private const val MillisPerSecond = 1000L

// The song whose menu is open, held outside the screens so any card can open it and the sheet can cover the whole window
object ItemMenu {
    private val mutableItem = MutableStateFlow<HomeItem?>(null)
    val item: StateFlow<HomeItem?> = mutableItem.asStateFlow()

    // The playlist of the account the song was opened from, which it can be taken out of
    private val mutablePlaylistId = MutableStateFlow<String?>(null)
    val playlistId: StateFlow<String?> = mutablePlaylistId.asStateFlow()

    // What takes the song out of the history, set when the song was opened from the history
    private val mutableRemoveFromHistory = MutableStateFlow<(() -> Unit)?>(null)
    val removeFromHistory: StateFlow<(() -> Unit)?> = mutableRemoveFromHistory.asStateFlow()

    // True when the song was opened from the list of the songs of the library, which it can be taken out of
    private val mutableRemovesFromLibrary = MutableStateFlow(false)
    val removesFromLibrary: StateFlow<Boolean> = mutableRemovesFromLibrary.asStateFlow()

    // Opens the menu of any card, a song, an artist, an album, or a playlist
    fun show(item: HomeItem, playlistId: String? = null, removeFromHistory: (() -> Unit)? = null, removesFromLibrary: Boolean = false) {
        mutableRemovesFromLibrary.value = removesFromLibrary
        mutablePlaylistId.value = playlistId
        mutableRemoveFromHistory.value = removeFromHistory
        mutableItem.value = item
    }

    fun dismiss() {
        mutableItem.value = null
    }
}

// Menu of a song of the Home, drawn over the whole screen, play next, add to the queue, save to a playlist, share and pin to the speed dial
@Composable
fun ItemMenuHost() {
    val item by ItemMenu.item.collectAsState()
    val current = item ?: return

    // An artist has its own menu
    if (current.kind == HomeItemKind.Artist) {
        ArtistMenu(current)
        return
    }

    // An album or a playlist has the menu of its page
    if (current.kind == HomeItemKind.Album || current.kind == HomeItemKind.Playlist) {
        val context = LocalContext.current
        LaunchedEffect(current) {
            ItemMenu.dismiss()
            showCollectionMenu(context, current)
        }
        return
    }
    val track = current.toPlayableTrack() ?: return
    val playlistId by ItemMenu.playlistId.collectAsState()
    val removeFromHistory by ItemMenu.removeFromHistory.collectAsState()
    val removesFromLibrary by ItemMenu.removesFromLibrary.collectAsState()
    val like = rememberLikeState(track.id)

    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    val pinnedIds by remember { PlayHistory.pinned(context).map { songs -> songs.map { it.id }.toSet() } }.collectAsState(initial = emptySet())
    val isPinned = track.id in pinnedIds

    // The album of a song comes with its links, asked when the menu opens so its name shows under its line and a tap opens it at once
    val links by produceState(RadioQueue.cachedLinks(track.id), track.id) {
        if (value == null && current.kind == HomeItemKind.Song && !isDeviceTrack(track.id)) value = RadioQueue.links(context, track.id).getOrNull()
    }

    SpectroSheet(onDismiss = ItemMenu::dismiss) {
        // The menu stays open for what changes something in it, and slides out only when the screen goes on to another one
        val closeSheet = LocalSheetClose.current

        // A station is live and has no place in a queue and a file of the phone has no radio and no playlist of the account
        val isRadio = isRadioTrack(track.id)
        val isDevice = isDeviceTrack(track.id)

        val hasPlayerComments = false

        // The song, its cover, its name and the three most used options
        val head: @Composable () -> Unit = {
            // The cover, the name and who made it, in the color of the accent, and the like on the end
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = ItemMenuDimens.HeaderVertical)
            ) {
                TrackCover(track = track, size = ItemMenuDimens.HeaderCover, modifier = Modifier.clip(RoundedCornerShape(ItemMenuDimens.HeaderCoverCorner)))
                Column(modifier = Modifier.weight(1f).padding(start = ItemMenuDimens.HeaderGap)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                    )
                    (track.artistLine() ?: itemSubtitle(current))?.let { line ->
                        Text(
                            text = line,
                            style = MaterialTheme.typography.bodyMedium,
                            color = SpectroTheme.colors.playerAccent,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                        )
                    }
                }
                if (!isDeviceTrack(track.id)) {
                    IconButton(onClick = like.toggle) {
                        Icon(
                            imageVector = if (like.isLiked) SpectroIcons.ThumbUpFilled else SpectroIcons.ThumbUp,
                            contentDescription = stringResource(R.string.player_favorite),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // The three most used options side by side
            if (!isRadio || !isDevice) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(ItemMenuDimens.CardTileGap),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = ItemMenuDimens.CardGroupGap - ItemMenuDimens.CardGap, bottom = ItemMenuDimens.CardGroupGap - ItemMenuDimens.CardGap)
                ) {
                    if (!isRadio) {
                        MenuCardTile(SpectroIcons.PlaylistPlay, stringResource(R.string.queue_play_next), Modifier.weight(1f)) {
                            if (PlayerConnection.playNextTrack(context, track)) Toasts.show(resources.getString(R.string.menu_playing_next))
                        }
                    }
                    if (!isDevice) {
                        MenuCardTile(SpectroIcons.PlaylistAdd, stringResource(R.string.playlist_save), Modifier.weight(1f)) {
                            closeSheet()
                            SaveToPlaylist.show(listOf(track.id))
                        }
                        MenuCardTile(SpectroIcons.Share, stringResource(R.string.player_share), Modifier.weight(1f)) {
                            shareSong(context, track.id)
                        }
                    }
                }
            }
        }

        // The other options, one card under the other
        val options: @Composable () -> Unit = {
            if (!isRadio) {
                MenuCard(SpectroIcons.QueueMusic, stringResource(R.string.menu_add_to_queue)) {
                    if (PlayerConnection.addTrackToQueue(context, track)) Toasts.show(resources.getString(R.string.menu_added_to_queue))
                }
            }
            if (!isDevice) {
                MenuCard(
                    SpectroIcons.Library,
                    stringResource(if (like.isLiked) R.string.collection_library_remove else R.string.collection_library_save)
                ) {
                    like.toggle()
                }
            }
            // The sleep timer, only in the menu of the song that plays, with what it is waiting for when it is on
            val playing by PlayerConnection.track.collectAsState()
            if (playing?.id == track.id) {
                MenuCard(
                    SpectroIcons.Equalizer,
                    stringResource(R.string.equalizer)
                ) {
                    closeSheet()
                    EqualizerSheet.show()
                }
                val sleep by SleepTimer.state.collectAsState()
                val left by produceState(initialValue = 0L, sleep) {
                    while (sleep.endsAtMs != null) {
                        value = SleepTimer.leftMs(sleep) ?: 0L
                        delay(SleepTickMillis)
                    }
                }
                MenuCard(
                    SpectroIcons.Timer,
                    when {
                        sleep.atEndOfSong -> stringResource(R.string.menu_sleep_timer_left, stringResource(R.string.sleep_until_end))
                        sleep.endsAtMs != null -> stringResource(R.string.menu_sleep_timer_left, formatTime(left / MillisPerSecond))
                        else -> stringResource(R.string.menu_sleep_timer)
                    }
                ) {
                    closeSheet()
                    SleepTimerSheet.show(context)
                }
            }

            // A file of the phone is not in YouTube Music, so it has no playlist of the account and no pin
            if (!isDevice) {
                playlistId?.let { id ->
                    MenuCard(SpectroIcons.Delete, stringResource(R.string.playlist_remove_song)) {
                        closeSheet()
                        // Kept apart from the menu, which leaves the screen before the answer comes
                        PlaylistChanges.emit(PlaylistEvent.SongRemoved(id, track.id, current.setVideoId))
                        PlaylistActions.scope.launch {
                            val result = PlaylistRepository(context).removeEntry(id, track.id, current.setVideoId)
                            val message = if (result.isSuccess) R.string.playlist_song_removed else R.string.playlist_action_error
                            Toasts.show(resources.getString(message), isError = result.isFailure)
                            // A refusal brings the song back
                            if (result.isFailure) PlaylistChanges.notifyChanged()
                        }
                    }
                }
                // A song opened from the list of the songs of the library can be taken out of it
                if (removesFromLibrary) {
                    MenuCard(SpectroIcons.Delete, stringResource(R.string.library_remove_song)) {
                        closeSheet()
                        Likes.removeAll(context, listOf(current))
                    }
                }
                // A song opened from the history can be taken out of it
                removeFromHistory?.let { remove ->
                    MenuCard(SpectroIcons.Delete, stringResource(R.string.history_remove)) {
                        closeSheet()
                        remove()
                    }
                }
                // The album and the artists of a song come from its links, asked now when they are not at hand, and the menu leaves when the page opens
                if (current.kind == HomeItemKind.Song) {
                    MenuCard(SpectroIcons.Person, stringResource(R.string.menu_go_to_artist), subtitle = track.artist) {
                        scope.launch {
                            val artists = (RadioQueue.cachedLinks(track.id) ?: RadioQueue.links(context, track.id).getOrNull())?.artists.orEmpty().filter { it.second.isNotEmpty() }
                            when {
                                artists.isEmpty() -> Toasts.show(resources.getString(R.string.menu_link_error), isError = true)
                                artists.size == 1 -> {
                                    closeSheet()
                                    ItemNavigator.openArtist(artists.first().second)
                                }
                                // Two or more artists are listed to pick from
                                else -> {
                                    closeSheet()
                                    OverlaySheet.show {
                                        ArtistPickerSheet(
                                            artists = artists,
                                            onArtistClick = { id ->
                                                OverlaySheet.dismiss()
                                                ItemNavigator.openArtist(id)
                                            },
                                            onDismiss = OverlaySheet::dismiss
                                        )
                                    }
                                }
                            }
                        }
                    }
                    MenuCard(SpectroIcons.Album, stringResource(R.string.menu_go_to_album), subtitle = links?.albumName) {
                        scope.launch {
                            val albumId = (RadioQueue.cachedLinks(track.id) ?: RadioQueue.links(context, track.id).getOrNull())?.albumId
                            if (albumId.isNullOrEmpty()) {
                                Toasts.show(resources.getString(R.string.menu_link_error), isError = true)
                            } else {
                                closeSheet()
                                ItemNavigator.openAlbum(albumId)
                            }
                        }
                    }
                }
                // Who sang, wrote and published the song, in a sheet of its own
                if (current.kind == HomeItemKind.Song && !isRadio) {
                    MenuCard(SpectroIcons.Group, stringResource(R.string.menu_credits)) {
                        closeSheet()
                        // The arrow of the credits brings this menu back
                        OverlaySheet.show {
                            SongCreditsSheet(
                                track = track,
                                onDismiss = OverlaySheet::dismiss,
                                onBack = { ItemMenu.show(current, playlistId, removeFromHistory, removesFromLibrary) }
                            )
                        }
                    }
                }
                // Tells YouTube Music to recommend less like this, only when it sent the way to say it
                current.notInterestedToken?.let { token ->
                    MenuCard(SpectroIcons.Block, stringResource(R.string.menu_not_interested)) {
                        closeSheet()
                        PlaylistActions.scope.launch {
                            val result = InnerTubeClient.feedback(currentSession(context), listOf(token))
                            val message = if (result.isSuccess) R.string.menu_not_interested_done else R.string.menu_not_interested_error
                            Toasts.show(resources.getString(message), isError = result.isFailure)
                        }
                    }
                }

                // Pinning to the speed dial comes last, a file of the phone is not in YouTube Music and has no pin
                MenuCard(SpectroIcons.Pin, stringResource(if (isPinned) R.string.menu_unpin else R.string.menu_pin)) {
                    scope.launch(Dispatchers.IO) {
                        if (isPinned) {
                            PlayHistory.unpin(context, track.id)
                        } else {
                            PlayHistory.pin(context, track.id, track.title, track.artist, track.artworkUrl, track.durationMs)
                        }
                    }
                }
            }
        }

        // Each option is a rounded card as the old Spectro drew them, the sheet keeps its own drag
        CardMenuLayout(head = head, options = options)
    }
}
