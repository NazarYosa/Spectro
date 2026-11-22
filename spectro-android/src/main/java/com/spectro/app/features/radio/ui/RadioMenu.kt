package com.spectro.app.features.radio.ui

// Android sharing
import android.content.Intent
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
// UI utilities
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
// Coroutines
import kotlinx.coroutines.launch
// Project resources
import com.spectro.app.R
import com.spectro.app.core.designsystem.components.LocalSheetClose
import com.spectro.app.core.designsystem.components.SpectroSheet
import com.spectro.app.core.designsystem.icons.SpectroIcons
import com.spectro.app.core.navigation.ItemNavigator
import com.spectro.app.core.radio.RadioLibrary
import com.spectro.app.core.radio.RadioSegments
import com.spectro.app.core.radio.RadioStation
import com.spectro.app.core.radio.RadiosOrigin
import com.spectro.app.features.menu.CardMenuHead
import com.spectro.app.features.menu.CardMenuLayout
import com.spectro.app.features.menu.MenuCard

// The three dots of a station, its logo and name on top, the favorites, the share and the genre of it, which opens the list of the stations of the same kind
@Composable
internal fun RadioMenu(station: RadioStation, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val favorites by remember { RadioLibrary.favorites(context) }.collectAsState(initial = emptyList())
    val isFavorite = favorites.any { it.id == station.id }

    // The genre the site says first, when it is one of the genres that have a list of their own
    val genres = rememberRadioGenres()
    val genre = station.segments.firstNotNullOfOrNull(RadioSegments::named)

    SpectroSheet(onDismiss = onDismiss) {
        val closeSheet = LocalSheetClose.current

        CardMenuLayout(
            head = { CardMenuHead(title = station.name, subtitle = station.place, coverUrl = station.logoUrl) },
            options = {
                MenuCard(
                    if (isFavorite) SpectroIcons.Favorite else SpectroIcons.FavoriteBorder,
                    stringResource(if (isFavorite) R.string.radios_favorite_remove else R.string.radios_favorite_add)
                ) {
                    scope.launch { RadioLibrary.toggleFavorite(context, station) }
                    closeSheet()
                }
                MenuCard(SpectroIcons.Share, stringResource(R.string.player_share)) {
                    closeSheet()
                    val address = if (station.url.startsWith("http")) station.url else RadiosOrigin + station.url
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, address)
                    }
                    context.startActivity(Intent.createChooser(intent, context.getString(R.string.player_share_via)))
                }
                genre?.let {
                    val title = genres.titleOf(it)
                    MenuCard(SpectroIcons.Radio, stringResource(R.string.radios_more_of, title)) {
                        closeSheet()
                        ItemNavigator.openRadiosPlace(title, it.path)
                    }
                }
            }
        )
    }
}
