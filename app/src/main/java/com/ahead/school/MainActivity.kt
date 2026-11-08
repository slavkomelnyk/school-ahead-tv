// MainActivity.kt
// =============================================================================
// Main activity for the School app.
// 
// This activity hosts the main UI and handles key events for TV navigation.
// 
// Updated to use the simplified hybrid architecture.
// =============================================================================

package com.ahead.school

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.KeyEvent
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
 * TV remote keys are routed through the activity so they work regardless of
 * whether the WebView or a Compose control currently has focus.
 */
class MainActivity : ComponentActivity() {

    private var appKeyEventHandler: ((KeyEvent) -> Boolean)? = null

    fun setAppKeyEventHandler(handler: ((KeyEvent) -> Boolean)?) {
        appKeyEventHandler = handler
    }

    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (appKeyEventHandler?.invoke(event) == true) return true
        return super.dispatchKeyEvent(event)
    }


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
