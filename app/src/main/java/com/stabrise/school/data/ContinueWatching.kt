package com.stabrise.school.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson

/** A video that was started and not watched to the end. */
data class UnfinishedVideo(
    val lessonId: Int,
    val subjectId: Int,
    val videoId: String,
    val title: String,
    val iconUrl: String?,
    val position: Float = 0f,
    val duration: Float = 0f
)

/** Unfinished videos saved on this device, the last watched first. */
object ContinueWatching {
    private const val KEY = "videos"
    private const val LIMIT = 20

    /** A video watched this far counts as fully watched. */
    private const val FINISHED_AT = 0.95f

    private var prefs: SharedPreferences? = null

    /** Call before use. */
    fun init(context: Context) {
        prefs = context.getSharedPreferences("continue_watching", Context.MODE_PRIVATE)
    }

    fun all(): List<UnfinishedVideo> = runCatching {
        Gson().fromJson(prefs?.getString(KEY, null), Array<UnfinishedVideo>::class.java)?.toList()
    }.getOrNull().orEmpty()

    fun isFinished(position: Float, duration: Float) =
        duration > 0 && position >= duration * FINISHED_AT

    /** Keeps the position of a video, or forgets the video once it is fully watched. */
    fun save(video: UnfinishedVideo) {
        val others = all().filter { it.lessonId != video.lessonId }
        val list = if (isFinished(video.position, video.duration)) others
        else (listOf(video) + others).take(LIMIT)
        prefs?.edit()?.putString(KEY, Gson().toJson(list))?.apply()
    }
}
