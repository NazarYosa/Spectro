package com.spectro.app.features.player.ui.components

// Compose layouts and foundations
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
// Project resources
import com.spectro.app.R
import com.spectro.app.core.device.artistLine
import com.spectro.app.core.device.isDeviceTrack
import com.spectro.app.core.designsystem.components.LocalSheetClose
import com.spectro.app.core.designsystem.components.SpectroSheet
import com.spectro.app.core.designsystem.icons.SpectroIcons
import com.spectro.app.core.designsystem.theme.DarkColors
import com.spectro.app.core.playback.QueueEntry
import com.spectro.app.features.menu.CardMenuHead
import com.spectro.app.features.menu.CardMenuLayout
import com.spectro.app.features.menu.MenuCard
import com.spectro.app.features.playlist.ui.SaveToPlaylist

// Options of a song of the queue, only what works for now, play next, save to a playlist, remove and share
@Composable
fun QueueItemMenu(
    entry: QueueEntry,
    canPlayNext: Boolean,
    canRemove: Boolean,
    onPlayNext: () -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val track = entry.track

    DarkColors {
        SpectroSheet(onDismiss = onDismiss) {
            val closeSheet = LocalSheetClose.current

            CardMenuLayout(
                head = {
                    CardMenuHead(
                        title = track.title,
                        subtitle = track.artistLine(),
                        coverUrl = track.artworkUrl
                    )
                },
                options = {
                    if (canPlayNext) {
                        MenuCard(SpectroIcons.PlaylistPlay, stringResource(R.string.queue_play_next)) {
                            onPlayNext()
                            closeSheet()
                        }
                    }
                    // A file of the phone is not in YouTube Music, so it has no playlist of the account and no page to share
                    if (!isDeviceTrack(track.id)) {
                        MenuCard(SpectroIcons.PlaylistAdd, stringResource(R.string.playlist_save)) {
                            closeSheet()
                            SaveToPlaylist.show(listOf(track.id))
                        }
                        MenuCard(SpectroIcons.Share, stringResource(R.string.player_share)) {
                            shareSong(context, track.id)
                            closeSheet()
                        }
                    }
                    if (canRemove) {
                        MenuCard(SpectroIcons.Delete, stringResource(R.string.queue_remove)) {
                            onRemove()
                            closeSheet()
                        }
                    }
                }
            )
        }
    }
}

private fun shareSong(context: android.content.Context, videoId: String) {
    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(android.content.Intent.EXTRA_TEXT, "https://music.youtube.com/watch?v=$videoId")
    }
    context.startActivity(android.content.Intent.createChooser(intent, null))
}
