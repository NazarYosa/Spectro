package com.spectro.app.features.player.ui.components

// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
// Material 3 components
import androidx.compose.material3.Icon
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
// Image loading
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.request.transformations
// Project resources
import com.spectro.app.core.designsystem.components.VideoSquareCrop
import com.spectro.app.core.designsystem.icons.SpectroIcons
import com.spectro.app.core.designsystem.theme.SpectroTheme
import com.spectro.app.core.innertube.isVideoThumbnail
import com.spectro.app.core.innertube.largestVideoThumbnail
import com.spectro.app.core.innertube.resize

// Arc where the progress ring starts, at the top
private const val RingStartAngle = -90f

// Mask that keeps the middle of the open cover and clears its edges
private val MaskKeep = Color.Black
private val MaskClear = Color.Transparent

// Cover that grows from the round one of the pill to the big one of the open player, over the blurred backdrop, as in the old Spectro
// The opening is read only while measuring and drawing, so the cover follows the finger without being built again
@Composable
fun AlbumCover(
    progress: () -> Float,
    coverAlpha: () -> Float,
    imageUrl: String?,
    songProgress: () -> Float,
    screenWidth: Dp,
    screenHeight: Dp,
    frame: LandscapeFrame?,
    panel: () -> Float
) {
    val context = LocalContext.current
    val colors = SpectroTheme.colors

    // Videos come wide, so their largest picture is asked and cut square, and the given one is used when the largest does not exist
    val isVideoPicture = imageUrl?.isVideoThumbnail() == true
    var useGivenPicture by remember(imageUrl) { mutableStateOf(false) }

    // The cover is always asked at full size, never at the size of the pill it starts in
    val coverRequest = remember(imageUrl, useGivenPicture) {
        imageUrl?.takeIf { it.isNotEmpty() }?.let { url ->
            val largest = if (useGivenPicture) null else url.largestVideoThumbnail()
            ImageRequest.Builder(context)
                .data(largest ?: url.resize(PlayerDimens.CoverRequestSize, PlayerDimens.CoverRequestSize))
                .size(PlayerDimens.CoverRequestSize)
                .crossfade(true)
                .apply { if (isVideoPicture) transformations(VideoSquareCrop) }
                .build()
        }
    }
    // Size and place of the open cover standing, lying down they come from the frame
    val expandedSize = (if (screenHeight < PlayerDimens.ShortScreenHeight) screenWidth * PlayerDimens.ShortScreenCoverFraction else screenWidth) - 32.dp
    val expandedX = (screenWidth - expandedSize) / 2
    val expandedY = screenHeight * PlayerDimens.ExpandedCoverTopFraction + 8.dp

    Box(modifier = Modifier.fillMaxSize()) {
        Backdrop(progress = progress)

        Box(
            modifier = Modifier
                .layout { measurable, constraints ->
                    val opening = progress()
                    val side = lerp(PlayerDimens.MiniCover, frame?.coverSize(panel()) ?: expandedSize, opening).roundToPx()
                    val placeable = measurable.measure(Constraints.fixed(side, side))
                    layout(constraints.maxWidth, constraints.maxHeight) {
                        placeable.place(
                            lerp(PlayerDimens.MiniCoverStart, frame?.coverX ?: expandedX, opening).roundToPx(),
                            lerp(PlayerDimens.MiniCoverTop, frame?.coverTop(panel()) ?: expandedY, opening).roundToPx()
                        )
                    }
                }
                .graphicsLayer {
                    val opening = progress()
                    // The cover fades out while the lyrics are open
                    alpha = coverAlpha()
                    val openCorner = if (frame != null) lerp(PlayerDimens.ExpandedCoverCorner, PlayerDimens.LandscapeSmallCoverCorner, panel()) else PlayerDimens.ExpandedCoverCorner
                    shape = RoundedCornerShape(lerp(PlayerDimens.MiniCoverCorner, openCorner, opening))
                    clip = true
                    // Lying down it is a picture with its edges, lifted from the backdrop, standing they fade into it, which needs its own layer, only while it shows
                    if (frame != null) {
                        shadowElevation = lerp(PlayerDimens.LandscapeCoverShadow, PlayerDimens.LandscapeSmallCoverShadow, panel()).toPx() * opening
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // The note shows under the picture until it arrives
            AlbumPlaceholder()
            if (coverRequest != null) {
                AsyncImage(
                    model = coverRequest,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    filterQuality = FilterQuality.High,
                    onError = { if (isVideoPicture) useGivenPicture = true },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

// Solid black background behind the open player
@Composable
private fun Backdrop(progress: () -> Float) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = progress() }
            .background(Color.Black)
    )
}

// Dark square with a faint note, shown while there is no picture
@Composable
internal fun AlbumPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SpectroTheme.colors.coverPlaceholder),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = SpectroIcons.MusicNote,
            contentDescription = null,
            tint = SpectroTheme.colors.onMedia.copy(alpha = PlayerDimens.PlaceholderIconAlpha),
            modifier = Modifier.fillMaxSize(PlayerDimens.PlaceholderIconFraction)
        )
    }
}
