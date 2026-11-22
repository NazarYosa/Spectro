package com.spectro.app.core.designsystem.theme

// Compose state and runtime
import androidx.compose.runtime.Immutable
// UI styling and utilities
import androidx.compose.ui.graphics.Color

// Colors that depend on the theme but are not part of the Material color scheme
@Immutable
data class SpectroColors(
    // Text and icons over images and gradients
    val onMedia: Color,
    // Dark fade under text over images
    val mediaScrim: Color,
    // Filled accent controls, darker than the accent so the white text stays readable
    val accentFill: Color,
    val onAccentFill: Color,
    // Surface for bars and sheets that sit above the background
    val elevated: Color,
    // Top bar of the Google sign in page, over the cutout and with the back button
    val loginBar: Color,
    // Background of the bottom navigation bar and of the side rail
    val navBar: Color,
    // Base of the mini player, which sits over the content and keeps its own color
    val miniPlayer: Color,
    // Round play button over a cover, and the dark that fades over the covers of the speed dial
    val playButton: Color,
    val tileScrim: Color,
    // Ring of the song progress around the cover of the mini player, its track, and the cover shown while there is no picture
    val playerRing: Color,
    val playerRingTrack: Color,
    val coverPlaceholder: Color,
    // Cyan of the expanded player, the red of a liked song, and the shadow under text over pictures
    val playerAccent: Color,
    val playerLiked: Color,
    val textShadow: Color,
    // Camera that marks the cover of a video
    val videoBadge: Color,
    // Push pin that marks what is pinned to the speed dial
    val pinBadge: Color,
    // Background of the filter chips and of the search field, a step apart from the surfaces so it shows over the glow
    val chip: Color,
    // Veil of the filters of the Home, faint so the glow shows through, white glass on the light theme
    val filterChip: Color,
    // Veil of the cards of the menus, a faint step of the text color over the sheet
    val menuCard: Color,
    // An item of a chart that went up
    val chartUp: Color,
    // Highlight color for badges and primary accents
    val host: Color,
    // How well the device reaches the internet, well and badly, none is the error color of the scheme
    val connectionGood: Color,
    val connectionWeak: Color,
    // How much of an episode was heard
    val episodeProgress: Color,
    // Loading placeholders and the light that sweeps over them
    val skeleton: Color,
    val skeletonHighlight: Color,
    // Icon of an unselected navigation item
    val navUnselected: Color,
    // Icon and name of the selected navigation item
    val navSelected: Color,
    // Soft glow at the top of full screens, blending from one side to the other
    val glowStart: Color,
    val glowEnd: Color,
    // Brand gradient from blue to violet to red, on the profile ring
    val gradientStart: Color,
    val gradientMiddle: Color,
    val gradientEnd: Color,
    // Main buttons, blue fading to a darker blue
    val buttonStart: Color,
    val buttonEnd: Color
)

// Extra colors of the dark theme (Spectro logo palette: Electric Violet, Radiant Rose Pink, and Vibrant Cyan)
val DarkSpectroColors = SpectroColors(
    onMedia = OnFilled,
    mediaScrim = Color(0xCC000000),
    accentFill = Color(0xFF24242E),
    onAccentFill = OnFilled,
    elevated = Color(0xFF181822),
    playButton = Color(0x99000000),
    tileScrim = Color(0xFF000000),
    playerRing = Color(0xFF8A4AF3),
    playerRingTrack = Color(0x26FAFAFC),
    coverPlaceholder = Color(0xFF181822),
    playerAccent = Color(0xFF8A4AF3),
    playerLiked = Color(0xFFFF4B72),
    textShadow = Color(0xFF000000),
    videoBadge = Color(0xFF8A4AF3),
    pinBadge = Color(0xFF8A4AF3),
    loginBar = Color(0xFF121216),
    navBar = Color(0xFF121216),
    miniPlayer = Color(0xFF181822),
    chip = Color(0xFF22222E),
    filterChip = Color(0xFF22222E),
    menuCard = Color(0xFF181822),
    chartUp = Color(0xFF00C2FF),
    host = Color(0xFFFFC83D),
    connectionGood = Color(0xFF00C2FF),
    connectionWeak = Color(0xFFFFC83D),
    episodeProgress = Color(0xFF8A4AF3),
    skeleton = Color(0xFF28282E),
    skeletonHighlight = Color(0xFF3E3E48),
    navUnselected = Color(0xFF8E8E9E),
    navSelected = Color(0xFF8A4AF3),
    glowStart = Color.Transparent,
    glowEnd = Color.Transparent,
    gradientStart = Color(0xFF8A4AF3),
    gradientMiddle = Color(0xFFFF4B72),
    gradientEnd = Color(0xFF00C2FF),
    buttonStart = Color(0xFF8A4AF3),
    buttonEnd = Color(0xFF6C38CC)
)

// Extra colors of the light theme
val LightSpectroColors = SpectroColors(
    onMedia = OnFilled,
    mediaScrim = Color(0x99000000),
    accentFill = LightAccent,
    onAccentFill = OnFilled,
    elevated = Color(0xFFE4E4E4),
    playButton = Color(0x99000000),
    tileScrim = Color(0xFF000000),
    playerRing = Color(0xFF6C5CE7),
    playerRingTrack = Color(0x1A000000),
    coverPlaceholder = Color(0xFFE8E8E8),
    playerAccent = Color(0xFF6C5CE7),
    playerLiked = Color(0xFFFF4B72),
    textShadow = Color(0xFF000000),
    videoBadge = Color(0xFF6C5CE7),
    pinBadge = Color(0xFF6C5CE7),
    loginBar = Color(0xFFFFFFFF),
    navBar = Color(0xFFFFFFFF),
    miniPlayer = Color(0xFFFFFFFF),
    chip = Color(0xFFD2D2D2),
    filterChip = Color(0xCCFFFFFF),
    menuCard = Color(0x0F000000),
    chartUp = Color(0xFF00C2FF),
    host = Color(0xFFB07A00),
    connectionGood = Color(0xFF00C2FF),
    connectionWeak = Color(0xFFB07A00),
    episodeProgress = Color(0xFF6C5CE7),
    skeleton = Color(0xFFCDCDCD),
    skeletonHighlight = Color(0xFFF2F2F2),
    navUnselected = Color(0xFF757575),
    navSelected = Color(0xFF6C5CE7),
    glowStart = Color.Transparent,
    glowEnd = Color.Transparent,
    gradientStart = Color(0xFF7C4DFF),
    gradientMiddle = Color(0xFFFF4B72),
    gradientEnd = Color(0xFF00C2FF),
    buttonStart = Color(0xFF6C5CE7),
    buttonEnd = Color(0xFF5E35B1)
)
