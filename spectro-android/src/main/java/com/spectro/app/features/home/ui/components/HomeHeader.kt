package com.spectro.app.features.home.ui.components

// Compose animation
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
// Compose layouts and foundations
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
// Material 3 components
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
// Project resources
import com.spectro.app.R
import com.spectro.app.core.designsystem.components.clickableNoIndication
import com.spectro.app.core.designsystem.icons.SpectroIcons
import com.spectro.app.core.designsystem.theme.SpectroTheme
import com.spectro.app.core.network.Connection
import com.spectro.app.core.network.rememberConnection
import com.spectro.app.core.history.PlayHistory
import com.spectro.app.features.profile.ui.LocalProfile
import com.spectro.app.features.profile.ui.components.ProfileAvatar

// How the device is connected, green when it reaches the internet, yellow blinking slowly when it reaches it badly or by fits, and both ways crossed in red when it does not
@Composable
private fun ConnectionIndicator(connection: Connection, modifier: Modifier = Modifier) {
    val colors = SpectroTheme.colors

    when (connection) {
        Connection.Wifi -> ConnectionIcon(SpectroIcons.Wifi, R.string.cd_wifi, colors.connectionGood, modifier)
        Connection.Cellular -> ConnectionIcon(SpectroIcons.Cellular, R.string.cd_cellular, colors.connectionGood, modifier)
        Connection.WifiWeak -> WeakConnectionIcon(SpectroIcons.Wifi, R.string.cd_wifi_weak, modifier)
        Connection.CellularWeak -> WeakConnectionIcon(SpectroIcons.Cellular, R.string.cd_cellular_weak, modifier)
        Connection.Offline -> Row(horizontalArrangement = Arrangement.spacedBy(HomeDimens.ConnectionPairGap), modifier = modifier) {
            ConnectionIcon(SpectroIcons.WifiOff, R.string.cd_offline, MaterialTheme.colorScheme.error)
            ConnectionIcon(SpectroIcons.CellularOff, null, MaterialTheme.colorScheme.error)
        }
    }
}

// One icon of the connection in the color that tells how it is
@Composable
private fun ConnectionIcon(icon: ImageVector, description: Int?, tint: Color, modifier: Modifier = Modifier) {
    Icon(
        imageVector = icon,
        contentDescription = description?.let { stringResource(it) },
        tint = tint,
        modifier = modifier.size(HomeDimens.ConnectionIcon)
    )
}

// The icon of a weak connection, which blinks slowly, the animation exists only while it shows so a good connection costs nothing
@Composable
private fun WeakConnectionIcon(icon: ImageVector, description: Int, modifier: Modifier = Modifier) {
    val blink by rememberInfiniteTransition(label = "WeakConnection").animateFloat(
        initialValue = 1f,
        targetValue = HomeDimens.ConnectionBlinkAlpha,
        animationSpec = infiniteRepeatable(tween(HomeDimens.ConnectionBlinkMillis, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "WeakConnectionBlink"
    )

    ConnectionIcon(icon, description, SpectroTheme.colors.connectionWeak, modifier.graphicsLayer { alpha = blink })
}

// Native header: Spectro title and connection on the left, profile on the right
@Composable
fun HomeHeader(
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier,
    onNotificationsClick: () -> Unit = {}
) {
    val dimens = SpectroTheme.dimens
    val profile = LocalProfile.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            .padding(horizontal = dimens.screenPadding, vertical = dimens.spaceMedium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Native app title and connection indicator
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 24.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )

            ConnectionIndicator(connection = rememberConnection(), modifier = Modifier.padding(start = HomeDimens.ConnectionGap))
        }

        // Profile button
        ProfileAvatar(
            photo = profile.photo,
            size = HomeDimens.ProfileButton,
            modifier = Modifier
                .clickableNoIndication(onClick = onProfileClick)
                .minimumInteractiveComponentSize()
        )
    }
}
