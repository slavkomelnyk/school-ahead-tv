// MediaState.kt
// =============================================================================
// Data class representing the state of media playback.
// =============================================================================

package com.ahead.school.model


/**
 * Represents the current state of media playback.
 * 
 * This data class holds all information needed to display and control
 * media playback in the UI, including:
 * - Whether media is present
 * - Playback state (playing/paused)
 * - Current position and duration
 * - Playback rate
 * - Source URL
 * - Fullscreen state
 * 
 * @param hasMedia Whether media is currently detected on the page
 * @param isPlaying Whether the media is currently playing
 * @param currentTime Current playback position in seconds
 * @param duration Total duration of the media in seconds
 * @param playbackRate Current playback rate (1.0 = normal speed)
 * @param source URL of the media source
 * @param isFullscreen Whether the media is currently in fullscreen mode
 */
data class MediaState(
    val hasMedia: Boolean = false,
    val isPlaying: Boolean = false,
    val currentTime: Float = 0f,
    val duration: Float = 0f,
    val playbackRate: Float = 1f,
    val source: String = "none",
    val isFullscreen: Boolean = false,
) {
    /**
     * Calculates the progress through the media as a value between 0 and 1.
     * 
     * @return Progress as a float between 0f and 1f
     */
    val progress: Float
        get() = if (duration > 0f) {
            (currentTime / duration).coerceIn(0f, 1f)
        } else {
            0f
        }
}
