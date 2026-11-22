package com.spectro.app.features.auth.ui.components

// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
// Material 3 components
import androidx.compose.material3.Icon
import androidx.compose.material3.minimumInteractiveComponentSize
// Compose state and runtime
import androidx.compose.runtime.Composable
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
// Project resources
import com.spectro.app.R
import com.spectro.app.core.designsystem.components.clickableNoIndication
import com.spectro.app.core.designsystem.icons.SpectroIcons
import com.spectro.app.core.designsystem.theme.SpectroTheme
import com.spectro.app.features.auth.ui.AuthDimens

// Round dark button with an arrow, for going back from a screen over a photo or a page
@Composable
fun RoundBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clickableNoIndication(onClick = onClick)
            .minimumInteractiveComponentSize()
            .size(AuthDimens.BackButton)
            .clip(CircleShape)
            .background(SpectroTheme.colors.mediaScrim),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = SpectroIcons.Back,
            contentDescription = stringResource(R.string.cd_back),
            tint = SpectroTheme.colors.onMedia
        )
    }
}
