package com.spectro.app.core.navigation

// Compose graphics and resources
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
// Project resources
import com.spectro.app.R
import com.spectro.app.core.designsystem.icons.SpectroIcons

// Main destinations shown in the navigation bar, in this order
enum class MainTab(
    val route: String,
    val icon: ImageVector,
    @StringRes val labelRes: Int
) {
    HOME("home", SpectroIcons.Home, R.string.nav_home),
    EXPLORE("explore", SpectroIcons.Explore, R.string.nav_explore),
    DISCOVER("discover", SpectroIcons.Discover, R.string.nav_discover),
    LIBRARY("library", SpectroIcons.Library, R.string.nav_library)
}
