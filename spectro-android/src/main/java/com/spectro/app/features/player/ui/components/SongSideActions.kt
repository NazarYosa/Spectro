package com.spectro.app.features.player.ui.components

// Android context, clipboard and toasts
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
// Compose animation
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
// Material 3 components
import androidx.compose.material3.Icon
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
// Project resources
import com.spectro.app.R
import com.spectro.app.core.designsystem.icons.SpectroIcons
import com.spectro.app.core.designsystem.theme.SpectroTheme
import com.spectro.app.core.device.isDeviceTrack
import com.spectro.app.core.innertube.MusicOrigin
import com.spectro.app.core.designsystem.components.Toasts

// Pill next to the title with share and favorite, always in the colors of the dark player, as in the old Spectro
@Composable
fun SongSideActions(
    songId: String?,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    // How much the heart shows, it goes while another one that travels takes its place
    favoriteAlpha: () -> Float = { 1f },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = SpectroTheme.colors

    // A file of the phone has no page to share and no like to give
    if (isDeviceTrack(songId)) return

    Row(
        modifier = modifier
            .padding(horizontal = PlayerDimens.SideActionsPaddingHorizontal, vertical = PlayerDimens.SideActionsPaddingVertical),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PlayerDimens.SideActionsSpacing)
    ) {
        AnimatedActionIcon(onClick = { copySongLink(context, songId) }) { scale ->
            Icon(
                imageVector = SpectroIcons.Link,
                contentDescription = stringResource(R.string.player_copy_link),
                tint = colors.onMedia,
                modifier = scale.size(PlayerDimens.ShareIcon)
            )
        }

        AnimatedActionIcon(onClick = onFavoriteClick) { scale ->
            Icon(
                imageVector = if (isFavorite) SpectroIcons.Favorite else SpectroIcons.FavoriteBorder,
                contentDescription = stringResource(R.string.player_favorite),
                tint = if (isFavorite) colors.playerLiked else colors.onMedia,
                modifier = scale.size(PlayerDimens.FavoriteIcon).graphicsLayer { alpha = favoriteAlpha() }
            )
        }
    }
}

// Icon that shrinks a little while pressed
@Composable
private fun AnimatedActionIcon(
    onClick: () -> Unit,
    content: @Composable (Modifier) -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) PlayerDimens.PressedScale else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "IconScale"
    )

    Box(
        modifier = Modifier
            .size(PlayerDimens.SideAction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content(
            Modifier.graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
        )
    }
}

// Copies the YouTube Music link of the song to the clipboard
internal fun copySongLink(context: Context, songId: String?) {
    if (songId.isNullOrEmpty()) {
        Toasts.show(context.getString(R.string.player_share_empty), isError = true)
        return
    }

    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    val clip = ClipData.newPlainText("Song Link", "$MusicOrigin/watch?v=$songId")
    clipboard?.setPrimaryClip(clip)
    Toasts.show(context.getString(R.string.player_link_copied))
}

// Shares the YouTube Music link of the song through the apps of the device
internal fun shareSong(context: Context, songId: String?) {
    if (songId.isNullOrEmpty()) {
        Toasts.show(context.getString(R.string.player_share_empty), isError = true)
        return
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, "$MusicOrigin/watch?v=$songId")
    }
    context.startActivity(Intent.createChooser(intent, context.getString(R.string.player_share_via)))
}
