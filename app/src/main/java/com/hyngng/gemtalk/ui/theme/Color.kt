package com.hyngng.gemtalk.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Semantic colors read the active light/dark Material palette at composition time. */
val Ink: Color @Composable get() = MaterialTheme.colorScheme.background
val Graphite: Color @Composable get() = MaterialTheme.colorScheme.surface
val GraphiteRaised: Color @Composable get() = MaterialTheme.colorScheme.surfaceVariant
val GraphiteSoft: Color @Composable get() = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
val Hairline: Color @Composable get() = MaterialTheme.colorScheme.outlineVariant

val Mist: Color @Composable get() = MaterialTheme.colorScheme.onSurface
val MistMuted: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
val MistFaint: Color @Composable get() = MaterialTheme.colorScheme.outline

val Jade: Color @Composable get() = MaterialTheme.colorScheme.primary
val JadeContent: Color @Composable get() = MaterialTheme.colorScheme.onPrimary
val JadeDeep: Color @Composable get() = MaterialTheme.colorScheme.primaryContainer
val Ice: Color @Composable get() = MaterialTheme.colorScheme.secondary
val Lumen: Color @Composable get() = MaterialTheme.colorScheme.tertiaryContainer

val UserBubble: Color @Composable get() = MaterialTheme.colorScheme.secondaryContainer
val AssistantBubble: Color @Composable get() = MaterialTheme.colorScheme.surface
val ErrorTint: Color @Composable get() = MaterialTheme.colorScheme.errorContainer
/** Platform-standard blue selection emphasis, independent of the app accent. */
val SelectionHighlight: Color @Composable get() = MaterialTheme.colorScheme.primaryContainer

// Stable values used outside composition, such as platform window and status colors.
val WindowInk = Color(0xFF0B0C0E)
