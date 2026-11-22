package com.spectro.app.core.designsystem.components

// Android resources
import androidx.annotation.StringRes
// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
// Material 3 components
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
// Project resources
import com.spectro.app.R
import com.spectro.app.core.designsystem.icons.SpectroIcons
import com.spectro.app.core.designsystem.theme.SpectroTheme

// The room a list keeps under its last row while songs are being marked, so the row can be scrolled clear of the bar
val SelectionBarSpace = 72.dp

// Top of a page while songs are being marked, the button that leaves, how many are marked and the button that marks all of them or none
@Composable
fun SelectionHeader(
    count: Int,
    allSelected: Boolean,
    onExit: () -> Unit,
    onToggleAll: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            .padding(horizontal = SpectroTheme.dimens.spaceSmall),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onExit) {
            Icon(SpectroIcons.Close, stringResource(R.string.queue_selection_exit), tint = MaterialTheme.colorScheme.onBackground)
        }
        Text(
            text = pluralStringResource(R.plurals.queue_selected, count, count),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
        IconButton(onClick = onToggleAll) {
            Icon(
                imageVector = SpectroIcons.Checklist,
                contentDescription = stringResource(if (allSelected) R.string.queue_select_none else R.string.queue_select_all),
                tint = if (allSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Bar of the actions for the marked songs, a hint of what to do stands in its place while none is marked
@Composable
fun SelectionBar(
    hasSelection: Boolean,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    modifier: Modifier = Modifier,
    // A file of the phone is not in YouTube Music, so its list has no playlist to save to
    onSave: (() -> Unit)? = null,
    // The list that already holds downloads takes them out in place of asking for them
    onDownload: (() -> Unit)? = null,
    onRemoveDownload: (() -> Unit)? = null,
    // Only a playlist of the account lets its songs be taken out
    onRemoveFromPlaylist: (() -> Unit)? = null,
    // The files of the phone leave it for good
    onDeleteFiles: (() -> Unit)? = null,
    // The songs of the library leave it, which takes the like away too
    onRemoveFromLibrary: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            // Touches on the bar never reach the songs behind it
            .pointerInput(Unit) { detectTapGestures { } }
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(SpectroTheme.dimens.spaceSmall),
        contentAlignment = Alignment.Center
    ) {
        if (hasSelection) {
            Row(horizontalArrangement = Arrangement.spacedBy(SpectroTheme.dimens.spaceExtraLarge), verticalAlignment = Alignment.CenterVertically) {
                SelectionAction(SpectroIcons.PlaylistPlay, R.string.queue_play_next, onPlayNext)
                SelectionAction(SpectroIcons.QueueMusic, R.string.menu_add_to_queue, onAddToQueue)
                onSave?.let { SelectionAction(SpectroIcons.PlaylistAdd, R.string.playlist_save, it) }
                onDownload?.let { SelectionAction(SpectroIcons.Download, R.string.download_do, it) }
                onRemoveDownload?.let { SelectionAction(SpectroIcons.Delete, R.string.download_remove, it) }
                onRemoveFromPlaylist?.let { SelectionAction(SpectroIcons.Delete, R.string.playlist_remove_song, it) }
                onDeleteFiles?.let { SelectionAction(SpectroIcons.Delete, R.string.library_device_delete, it) }
                onRemoveFromLibrary?.let { SelectionAction(SpectroIcons.Delete, R.string.library_remove_song, it) }
            }
        } else {
            Text(
                text = stringResource(R.string.queue_select_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = SpectroTheme.dimens.spaceMedium)
            )
        }
    }
}

// One button of the bar
@Composable
private fun SelectionAction(icon: ImageVector, @StringRes label: Int, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(icon, stringResource(label), tint = MaterialTheme.colorScheme.onSurface)
    }
}
