package com.spectro.app.core.designsystem.theme

// Material 3 components
import androidx.compose.material3.Typography
// UI styling and utilities
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Native system font family
val NativeFont = FontFamily.Default
val Poppins = FontFamily.Default

// All sizes in sp so text follows the system font size
val Typography = Typography(
    // Name on the welcome screen
    displaySmall = TextStyle(
        fontFamily = NativeFont,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    ),
    // Wordmark in the header
    headlineMedium = TextStyle(
        fontFamily = NativeFont,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.5).sp
    ),
    // Greeting
    headlineSmall = TextStyle(
        fontFamily = NativeFont,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 30.sp,
        letterSpacing = 0.sp
    ),
    // Section and screen titles
    titleLarge = TextStyle(
        fontFamily = NativeFont,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.5).sp
    ),
    // Titles inside big cards
    titleMedium = TextStyle(
        fontFamily = NativeFont,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp
    ),
    // Song, album and show titles on cards
    titleSmall = TextStyle(
        fontFamily = NativeFont,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = NativeFont,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.2.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = NativeFont,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    // Buttons
    labelLarge = TextStyle(
        fontFamily = NativeFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.1.sp
    ),
    // Chips, artist names and subtitles
    labelMedium = TextStyle(
        fontFamily = NativeFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.3.sp
    ),
    // Badges and small captions
    labelSmall = TextStyle(
        fontFamily = NativeFont,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.2.sp
    )
)
