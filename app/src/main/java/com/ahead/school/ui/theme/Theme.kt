// Theme.kt
// =============================================================================
// Material Design 3 theme for TV.
// =============================================================================

package com.ahead.school.ui.theme

import androidx.compose.runtime.Composable
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme


/**
 * TV-specific color scheme using School color palette.
 */
private val TvColorScheme = darkColorScheme(
    // Primary colors
    primary = SchoolBlue,
    onPrimary = SchoolOnPrimary,
    primaryContainer = SchoolBlueContainer,
    onPrimaryContainer = SchoolOnPrimary,
    
    // Secondary colors
    secondary = SchoolBlue,
    onSecondary = SchoolOnPrimary,
    secondaryContainer = SchoolSurfaceVariant,
    onSecondaryContainer = SchoolOnSurfaceVariant,
    
    // Background colors
    background = SchoolBlack,
    onBackground = SchoolOnSurface,
    
    // Surface colors
    surface = SchoolSurface,
    onSurface = SchoolOnSurface,
    surfaceVariant = SchoolSurfaceVariant,
    onSurfaceVariant = SchoolSecondaryText,
    
    // Error colors
    error = SchoolError,
    onError = SchoolOnError,
    errorContainer = SchoolErrorContainer,
    onErrorContainer = SchoolOnError,
)

/**
 * School app theme.
 * 
 * This composable applies the School color scheme and typography to
 * the content.
 * 
 * @param content The content to display within the theme
 */
@Composable
fun SchoolTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TvColorScheme,
        typography = TvTypography,
        content = content,
    )
}
