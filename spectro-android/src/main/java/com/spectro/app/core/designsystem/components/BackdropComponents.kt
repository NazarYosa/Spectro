package com.spectro.app.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.request.transformations
import com.spectro.app.core.designsystem.theme.SpectroMotion
import com.spectro.app.core.innertube.resize
import com.spectro.app.features.collection.ui.CollectionDimens
import com.spectro.app.features.discover.ui.DiscoverDimens
import com.spectro.app.features.player.ui.components.BackdropBlur
import com.spectro.app.features.player.ui.components.PlayerDimens

@Composable
fun CoverBackdrop(coverUrl: String?, listState: LazyListState, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val pageColor = MaterialTheme.colorScheme.background
    val isLight = pageColor.luminance() > 0.5f
    val backdropFade = rememberBackdropFade(listState) { listState.layoutInfo.viewportSize.height * CollectionDimens.BackdropFadeFraction }

    val backdrop = remember(coverUrl) {
        coverUrl?.let { url ->
            ImageRequest.Builder(context)
                .data(url.resize(PlayerDimens.BackdropRequestSize, PlayerDimens.BackdropRequestSize))
                .size(PlayerDimens.BackdropRequestSize)
                .transformations(BackdropBlur(PlayerDimens.BackdropBlurRadius, PlayerDimens.BackdropBlurPasses))
                .crossfade(SpectroMotion.ScreenFadeMillis)
                .build()
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(CollectionDimens.BackdropFraction)
            .graphicsLayer {
                alpha = backdropFade()
            }
    ) {
        if (backdrop != null) {
            AsyncImage(
                model = backdrop,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().alpha(if (isLight) CollectionDimens.BackdropLightAlpha else 1f)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to if (isLight) Color.Transparent else Color.Black.copy(alpha = CollectionDimens.BackdropTopShade),
                        CollectionDimens.BackdropHold to Color.Transparent,
                        1f to pageColor
                    )
                )
        )
    }
}

@Composable
fun HubBackdrop(coverUrl: String?, accent: Color, fade: () -> Float, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val pageColor = MaterialTheme.colorScheme.background
    val isLight = pageColor.luminance() > 0.5f

    val backdrop = remember(coverUrl) {
        coverUrl?.let { url ->
            ImageRequest.Builder(context)
                .data(url.resize(PlayerDimens.BackdropRequestSize, PlayerDimens.BackdropRequestSize))
                .size(PlayerDimens.BackdropRequestSize)
                .transformations(BackdropBlur(PlayerDimens.BackdropBlurRadius, PlayerDimens.BackdropBlurPasses))
                .crossfade(SpectroMotion.ScreenFadeMillis)
                .build()
        }
    }
    val strength = if (isLight) DiscoverDimens.HubBackdropLightAlpha else DiscoverDimens.HubBackdropAlpha

    Box(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(DiscoverDimens.HubBackdropFraction)
            .graphicsLayer { alpha = fade() }
    ) {
        if (backdrop != null) {
            AsyncImage(
                model = backdrop,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().graphicsLayer { alpha = strength }
            )
        } else {
            Box(modifier = Modifier.fillMaxSize().background(accent.copy(alpha = strength)))
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(DiscoverDimens.HubBackdropHold to Color.Transparent, 1f to pageColor))
        )
    }
}
