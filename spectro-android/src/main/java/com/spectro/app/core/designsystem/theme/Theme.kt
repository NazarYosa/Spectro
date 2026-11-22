package com.spectro.app.core.designsystem.theme

// Android components
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
// Compose animation and layouts
import androidx.compose.foundation.isSystemInDarkTheme
// Material 3 components
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
// UI styling and utilities
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Material colors of the dark theme
private val DarkColorScheme = darkColorScheme(
    primary = DarkAccent,
    onPrimary = DarkOnAccent,
    primaryContainer = DarkAccentContainer,
    onPrimaryContainer = DarkOnAccentContainer,
    inversePrimary = LightAccent,
    secondary = DarkOnSurfaceVariant,
    onSecondary = DarkOnAccent,
    secondaryContainer = DarkContainerHighest,
    onSecondaryContainer = DarkOnBackground,
    tertiary = DarkAccent,
    onTertiary = DarkOnAccent,
    tertiaryContainer = DarkAccentContainer,
    onTertiaryContainer = DarkOnAccentContainer,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkBackground,
    onSurface = DarkOnBackground,
    surfaceVariant = DarkContainer,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceTint = DarkAccent,
    inverseSurface = DarkOnBackground,
    inverseOnSurface = DarkBackground,
    error = DarkError,
    onError = OnFilled,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    scrim = Scrim,
    surfaceBright = DarkContainerHighest,
    surfaceDim = DarkContainerLowest,
    surfaceContainer = DarkContainer,
    surfaceContainerHigh = DarkContainerHigh,
    surfaceContainerHighest = DarkContainerHighest,
    surfaceContainerLow = DarkContainerLow,
    surfaceContainerLowest = DarkContainerLowest
)

// Material colors of the light theme
private val LightColorScheme = lightColorScheme(
    primary = LightAccent,
    onPrimary = LightOnAccent,
    primaryContainer = LightAccentContainer,
    onPrimaryContainer = LightOnAccentContainer,
    inversePrimary = DarkAccent,
    secondary = LightOnSurfaceVariant,
    onSecondary = LightOnAccent,
    secondaryContainer = LightContainerHigh,
    onSecondaryContainer = LightOnBackground,
    tertiary = LightAccent,
    onTertiary = LightOnAccent,
    tertiaryContainer = LightAccentContainer,
    onTertiaryContainer = LightOnAccentContainer,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightBackground,
    onSurface = LightOnBackground,
    surfaceVariant = LightContainer,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceTint = LightAccent,
    inverseSurface = LightOnBackground,
    inverseOnSurface = LightBackground,
    error = LightError,
    onError = OnFilled,
    errorContainer = LightErrorContainer,
    onErrorContainer = LightOnErrorContainer,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    scrim = Scrim,
    surfaceBright = LightContainerLowest,
    surfaceDim = LightContainerHighest,
    surfaceContainer = LightContainer,
    surfaceContainerHigh = LightContainerHigh,
    surfaceContainerHighest = LightContainerHighest,
    surfaceContainerLow = LightContainerLow,
    surfaceContainerLowest = LightContainerLowest
)

// Extra colors of the current theme
private val LocalSpectroColors = staticCompositionLocalOf { DarkSpectroColors }

// App theme, follows the system unless a mode is forced
@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun SpectroTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val spectroColors = if (darkTheme) DarkSpectroColors else LightSpectroColors
    val dimens = dimensFor(windowWidthFor(LocalConfiguration.current.screenWidthDp))

    SystemBarsAppearance(darkTheme = darkTheme)

    CompositionLocalProvider(
        LocalDimens provides dimens,
        LocalSpectroColors provides spectroColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = SpectroShapes,
            content = content
        )
    }
}

// Dark colors only, for the expanded player that always sits over a dark picture, the system bars keep the app theme
@Composable
fun DarkColors(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalSpectroColors provides DarkSpectroColors
    ) {
        MaterialTheme(
            colorScheme = DarkColorScheme,
            typography = Typography,
            shapes = SpectroShapes,
            content = content
        )
    }
}

// Shortcuts to read the app tokens
object SpectroTheme {
    val colors: SpectroColors
        @Composable get() = LocalSpectroColors.current

    val dimens: Dimens
        @Composable get() = LocalDimens.current
}

// Icons of the system bars follow the theme, they show up when the bars come back with a swipe
@Composable
private fun SystemBarsAppearance(darkTheme: Boolean) {
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = view.context.findActivity()?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }
}

// Activity behind a context that may be wrapped
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
