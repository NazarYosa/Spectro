package com.spectro.app.features.player.ui.components

// Compose animation
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
// Material 3 components
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
// Project resources
import com.spectro.app.R
import com.spectro.app.core.designsystem.icons.SpectroIcons
import com.spectro.app.core.designsystem.theme.SpectroTheme
import com.spectro.app.core.lyrics.LyricsSettings

// Lyrics over the open player with the song name above them, a tap closes them, as in the old Spectro, with the menu of the lyrics options
@Composable
fun PlayerLyricsOverlay(
    visible: Boolean,
    frame: LandscapeFrame?,
    state: LyricsState,
    settings: LyricsSettings,
    positionMs: () -> Long,
    title: String,
    artist: String,
    onOptionsClick: () -> Unit,
    onSeek: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = SpectroTheme.colors

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(if (frame != null) tween(PlayerDimens.LandscapeSideEnterMillis, PlayerDimens.LandscapeSideEnterDelay) else tween(PlayerDimens.LyricsFadeMillis)),
        exit = fadeOut(tween(if (frame != null) PlayerDimens.LandscapeSideExitMillis else PlayerDimens.LyricsFadeMillis))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .then(
                    if (frame != null) {
                        Modifier.padding(start = frame.sideStart, end = frame.sideEnd)
                    } else {
                        Modifier.padding(top = PlayerDimens.LyricsTop, bottom = PlayerDimens.LyricsBottom)
                    }
                )
                // Drags stay in the lyrics, so they do not close the player
                .pointerInput(Unit) { detectDragGestures { change, _ -> change.consume() } }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        ) {
            LyricsView(
                state = state,
                settings = settings,
                positionMs = positionMs,
                onSeek = onSeek,
                isLandscape = frame != null,
                modifier = if (frame != null) {
                    Modifier
                        .padding(top = frame.barBottom, bottom = frame.queueListBottom)
                        .fadingEnds(PlayerDimens.LyricsFadeFraction)
                } else {
                    Modifier.padding(top = PlayerDimens.LyricsTop)
                }
            )

            // Lying down the name of the song is already beside the lyrics, so their own name goes on the line of the tools
            // Their options are the only button of the line at the bottom, at its end where the bar of the queue has its buttons
            val currentSourceName = (state as? LyricsState.Ready)?.lyrics?.sourceName ?: "YouTube Music"

            if (frame != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.sideHeader(frame).fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.lyrics_title),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = PlayerDimens.LandscapeSideTitleSize),
                            color = colors.onMedia
                        )
                        Text(
                            text = "Sumber: $currentSourceName",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFC0C0C0)
                        )
                    }
                }

                IconButton(
                    onClick = onOptionsClick,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = frame.queueBarBottom)
                        .offset(x = PlayerDimens.QueueBarToolsShift)
                        .requiredSize(PlayerDimens.LandscapeToolButton)
                ) {
                    Icon(
                        imageVector = SpectroIcons.Settings,
                        contentDescription = stringResource(R.string.lyrics_options),
                        tint = colors.onMedia.copy(alpha = PlayerDimens.LandscapeToolAlpha),
                        modifier = Modifier.size(PlayerDimens.LandscapeToolIcon)
                    )
                }
                return@AnimatedVisibility
            }

            // Top Header: Single Close Arrow + Centered Lyrics Source ("BetterLyrics") + Action Buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = SpectroIcons.ArrowDown,
                        contentDescription = stringResource(R.string.player_minimize),
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = currentSourceName,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = Color(0xFFAAAAAA),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onOptionsClick, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = SpectroIcons.MoreVertical,
                        contentDescription = stringResource(R.string.lyrics_options),
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
