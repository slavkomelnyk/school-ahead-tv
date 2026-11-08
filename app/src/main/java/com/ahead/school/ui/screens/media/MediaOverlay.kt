// MediaOverlay.kt
// =============================================================================
// Overlay UI for controlling media playback.
// 
// This composable displays a semi-transparent overlay with playback controls
// when a media element is detected on the page.
// =============================================================================

package com.ahead.school.ui.screens.media

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.ahead.school.R
import com.ahead.school.model.MediaState
import com.ahead.school.ui.components.TvPrimaryButton
import com.ahead.school.ui.theme.Dimens
import com.ahead.school.util.formatTime


/**
 * Media overlay component for controlling playback.
 * 
 * This component displays playback controls (close, play/pause, fullscreen)
 * when media is detected on the page. The overlay has a semi-transparent background
 * and appears over the WebView content.
 * 
 * @param state The current media state
 * @param visible Whether the overlay should be visible
 * @param onPlayPause Callback when play/pause button is clicked
 * @param onFullscreen Callback when fullscreen button is clicked
 * @param onBack Callback when back button is clicked
 * @param onCloseOverlay Callback when close button is clicked
 * @param modifier Modifier for the composable
 */
@Composable
fun MediaOverlay(
    state: MediaState,
    visible: Boolean,
    onPlayPause: () -> Unit,
    onFullscreen: () -> Unit,
    onBack: () -> Unit,
    onCloseOverlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Focus requester for the play/pause button
    val playFocus = remember { FocusRequester() }
    
    // Track the requested play state to handle rapid toggles
    var requestedPlaying by remember(state.source) { mutableStateOf(state.isPlaying) }

    // Update requested state when actual state changes
    LaunchedEffect(state.isPlaying) {
        requestedPlaying = state.isPlaying
    }

    // Request focus when overlay becomes visible
    LaunchedEffect(visible) {
        if (visible) {
            withFrameNanos { }
            playFocus.requestFocus()
        }
    }

    // ---------------------------------------------------------------------------
    // Main Overlay UI
    // ---------------------------------------------------------------------------

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        // Main overlay container - transparent background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent),
        ) {
            // Bottom controls panel with semi-transparent background
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                    .padding(horizontal = Dimens.OverlayPadding, vertical = 16.dp),
            ) {
                // Small control buttons row - optimized for slow TVs
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 8.dp),
                ) {
                    // Close button - small, to hide overlay
                    TvPrimaryButton(
                        text = stringResource(R.string.media_close),
                        onClick = onCloseOverlay,
                        compact = true,
                        modifier = Modifier.height(36.dp),
                    )

                    // Play/Pause button - small
                    TvPrimaryButton(
                        text = stringResource(
                            if (requestedPlaying) R.string.media_pause
                            else R.string.media_play
                        ),
                        onClick = {
                            requestedPlaying = !requestedPlaying
                            onPlayPause()
                        },
                        focusRequester = playFocus,
                        compact = true,
                        modifier = Modifier.height(36.dp),
                    )

                    // Fullscreen button - small (only show when not already in fullscreen)
                    if (!state.isFullscreen) {
                        TvPrimaryButton(
                            text = stringResource(R.string.media_maximize),
                            onClick = onFullscreen,
                            compact = true,
                            modifier = Modifier.height(36.dp),
                        )
                    }

                    // Time display - small, only show when duration is available
                    if (state.duration > 0f) {
                        Text(
                            text = "${formatTime(state.currentTime)} / ${formatTime(state.duration)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(start = 16.dp),
                        )
                    }
                }
            }

            // -----------------------------------------------------------------------
            // Top-left: Back button (when in fullscreen)
            // -----------------------------------------------------------------------
            if (state.isFullscreen) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp),
                ) {
                    TvPrimaryButton(
                        text = stringResource(R.string.media_back),
                        onClick = onBack,
                        compact = true,
                    )
                }
            }
        }
    }
}
