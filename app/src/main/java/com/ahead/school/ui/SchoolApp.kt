// SchoolApp.kt
// =============================================================================
// Main composable for the School app.
// 
// Updated to use the hybrid TvWebViewScreen that combines School's
// reliability with School2's features.
// =============================================================================

package com.ahead.school.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ahead.school.ui.screens.web.TvWebViewScreen


/**
 * Main composable for the School app.
 * 
 * This component now uses the hybrid TvWebViewScreen which combines:
 * - School's reliable media bridge and clean architecture
 * - School2's always-visible overlay
 * - School2's no-tinting design
 * - School2's enhanced YouTube support
 * 
 * @param onClose Callback when the app should close
 * @param modifier Modifier for the composable
 */
@Composable
fun SchoolApp(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Use the hybrid TvWebViewScreen
        TvWebViewScreen(
            url = "https://school.stabrise.com/uk/subjects",
            modifier = Modifier.fillMaxSize()
        )
    }
}
