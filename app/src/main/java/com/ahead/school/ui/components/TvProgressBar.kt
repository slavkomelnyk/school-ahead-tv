// TvProgressBar.kt
// =============================================================================
// A D-pad-friendly progress bar for media playback.
// =============================================================================

package com.ahead.school.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import com.ahead.school.ui.theme.Dimens
import com.ahead.school.ui.theme.SchoolFocusOutline


/**
 * A D-pad-friendly progress bar for media playback.
 * 
 * When focused, users can use Left/Right keys to seek by [step] seconds.
 * The progress bar has a circular thumb that can be moved by arrow keys.
 * 
 * @param progress The current progress (0f to 1f)
 * @param onSeekBy Callback when seeking by delta seconds
 * @param modifier Modifier for the progress bar
 * @param step The number of seconds to seek when using arrow keys
 */
@Composable
fun TvProgressBar(
    progress: Float,
    onSeekBy: (Double) -> Unit,
    modifier: Modifier = Modifier,
    step: Float = 5f,
) {
    val clamped = progress.coerceIn(0f, 1f)
    var isFocused by remember { mutableStateOf(false) }
    
    // Use TV Material 3 colors
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val progressColor = MaterialTheme.colorScheme.primary
    val thumbColor = MaterialTheme.colorScheme.primary
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(Dimens.ProgressThumb)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .onKeyEvent { event ->
                // Only handle key down events
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                
                when (event.key) {
                    Key.DirectionLeft -> {
                        onSeekBy(-step.toDouble())
                        true
                    }
                    Key.DirectionRight -> {
                        onSeekBy(step.toDouble())
                        true
                    }
                    else -> false
                }
            }
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = if (isFocused) SchoolFocusOutline else Color.Transparent,
                shape = RoundedCornerShape(50)
            ),
        contentAlignment = Alignment.CenterStart,
    ) {
        // Track (background)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(Dimens.ProgressTrack)
                .background(trackColor, RoundedCornerShape(50)),
        )
        
        // Fill (progress)
        Box(
            modifier = Modifier
                .fillMaxWidth(clamped)
                .height(Dimens.ProgressTrack)
                .background(progressColor, RoundedCornerShape(50)),
        )
        
        // Thumb (draggable indicator)
        Box(
            modifier = Modifier
                .fillMaxWidth(clamped)
                .height(Dimens.ProgressThumb),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Box(
                modifier = Modifier
                    .size(Dimens.ProgressThumb)
                    .background(thumbColor, CircleShape),
            )
        }
    }
}
