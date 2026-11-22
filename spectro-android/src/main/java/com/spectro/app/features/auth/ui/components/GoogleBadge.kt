package com.spectro.app.features.auth.ui.components

// Compose layouts and foundations
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
// Compose state and runtime
import androidx.compose.runtime.Composable
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
// Project resources
import com.spectro.app.R
import com.spectro.app.core.designsystem.theme.SpectroTheme
import com.spectro.app.features.auth.ui.AuthDimens

// Google logo on a white circle, so its colors keep their contrast over the blue button
@Composable
fun GoogleBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(AuthDimens.GoogleBadge)
            .background(SpectroTheme.colors.onAccentFill, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.google_g),
            contentDescription = null,
            modifier = Modifier.size(AuthDimens.GoogleLogo)
        )
    }
}
