package com.spectro.app.core.designsystem.theme

// Compose drawing and utilities
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

// Native theme: no artificial colored aura/glow, keeping a clean solid native background
@Composable
fun Modifier.backgroundGlow(strength: () -> Float = { 1f }): Modifier = this
