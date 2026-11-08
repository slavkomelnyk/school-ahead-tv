// Dimens.kt
// =============================================================================
// Dimensions for the School app (TV-optimized for 10-foot UI).
// =============================================================================

package com.ahead.school.ui.theme

import androidx.compose.ui.unit.dp


/**
 * TV-optimized dimensions for 10-foot UI.
 * 
 * All dimensions are larger than typical mobile dimensions to ensure
 * readability and usability from a distance.
 */
object Dimens {
    
    // =========================================================================
    // Screen and Overlay Padding
    // =========================================================================
    
    /**
     * Standard screen padding for TV.
     */
    val ScreenPadding = 64.dp
    
    /**
     * Padding for overlay elements (e.g., media overlay).
     */
    val OverlayPadding = 64.dp

    
    // =========================================================================
    // Button Dimensions
    // =========================================================================
    
    /**
     * Standard button height for TV.
     */
    val ButtonHeight = 56.dp
    
    /**
     * Compact button height for secondary buttons.
     */
    val ButtonHeightCompact = 48.dp
    
    /**
     * Horizontal padding inside buttons.
     */
    val ButtonPaddingHorizontal = 32.dp
    
    /**
     * Minimum width for buttons to ensure touch targets are large enough.
     */
    val ButtonMinWidth = 140.dp

    
    // =========================================================================
    // Progress Bar Dimensions
    // =========================================================================
    
    /**
     * Height of the progress track.
     */
    val ProgressTrack = 8.dp
    
    /**
     * Size of the progress thumb (draggable indicator).
     */
    val ProgressThumb = 20.dp

    
    // =========================================================================
    // Icon Sizes
    // =========================================================================
    
    /**
     * Large icon size.
     */
    val IconLarge = 64.dp
    
    /**
     * Medium icon size.
     */
    val IconMedium = 48.dp
    
    /**
     * Small icon size.
     */
    val IconSmall = 32.dp

    
    // =========================================================================
    // Spacing
    // =========================================================================
    
    /**
     * Small spacing (e.g., between closely related elements).
     */
    val SpacingSmall = 12.dp
    
    /**
     * Medium spacing (e.g., between elements in a group).
     */
    val SpacingMedium = 20.dp
    
    /**
     * Large spacing (e.g., between different sections).
     */
    val SpacingLarge = 32.dp
    
    /**
     * Extra large spacing (e.g., between major sections).
     */
    val SpacingXLarge = 48.dp

    
    // =========================================================================
    // Corner Radii
    // =========================================================================
    
    /**
     * Small corner radius (e.g., for cards).
     */
    val CornerRadiusSmall = 8.dp
    
    /**
     * Medium corner radius (e.g., for buttons).
     */
    val CornerRadiusMedium = 12.dp
    
    /**
     * Large corner radius (e.g., for large cards).
     */
    val CornerRadiusLarge = 16.dp
    
    /**
     * Full corner radius for pill-shaped elements.
     */
    val CornerRadiusFull = 50.dp
}
