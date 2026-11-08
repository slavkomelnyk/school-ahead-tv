// MainActivity.kt
// =============================================================================
// Main activity for the School app.
// 
// This activity hosts the main UI and handles key events for TV navigation.
// 
// Updated to use the simplified hybrid architecture.
// =============================================================================

package com.ahead.school

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.ahead.school.ui.SchoolApp
import com.ahead.school.ui.theme.SchoolTheme


/**
 * Main activity for the School app.
 * 
 * This activity:
 * - Hosts the main composable UI (SchoolApp)
 * - Keeps screen on during video playback
 * 
 * The key handling is now done in the TvWebViewScreen component.
 */
class MainActivity : ComponentActivity() {


    // ---------------------------------------------------------------------------
    // Lifecycle
    // ---------------------------------------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Keep screen on during video playback
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            SchoolTheme {
                SchoolApp(
                    onClose = { finish() },
                )
            }
        }
    }
}
