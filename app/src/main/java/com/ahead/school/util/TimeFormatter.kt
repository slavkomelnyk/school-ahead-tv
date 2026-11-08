// TimeFormatter.kt
// =============================================================================
// Utility for formatting time values for display.
// =============================================================================

package com.ahead.school.util

import java.util.Locale


/**
 * Formats a time duration in seconds to a human-readable string.
 * 
 * This function handles both short durations (MM:SS) and long durations
 * (HH:MM:SS) and ensures the output is always valid.
 * 
 * @param seconds The time duration in seconds
 * @return Formatted time string (e.g., "01:23:45" or "05:30")
 */
fun formatTime(seconds: Float): String {
    if (seconds.isNaN() || seconds < 0f) return "00:00"
    
    val s = seconds.toInt()
    
    return if (s >= 3600) {
        // Long format: HH:MM:SS
        String.format(Locale.US, "%d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60)
    } else {
        // Short format: MM:SS
        String.format(Locale.US, "%02d:%02d", s / 60, s % 60)
    }
}
