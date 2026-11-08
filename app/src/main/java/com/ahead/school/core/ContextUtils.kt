// ContextUtils.kt
// =============================================================================
// Extension functions for Android Context.
// =============================================================================

package com.ahead.school.core

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import java.io.IOException


/**
 * Reads the contents of an asset file as a string.
 * 
 * @param path The path to the asset file (relative to assets directory)
 * @return The contents of the file, or empty string if not found or error occurs
 */
fun Context.readAsset(path: String): String = try {
    assets.open(path).bufferedReader().use { it.readText() }
} catch (_: IOException) {
    ""
}

/**
 * Finds the Activity in a Context hierarchy.
 * 
 * This is useful when you have a Context that might be a View or Service
 * and you need to find the containing Activity.
 * 
 * @return The Activity, or null if not found in the hierarchy
 */
fun Context.findActivity(): Activity? {
    var ctx: Context = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
