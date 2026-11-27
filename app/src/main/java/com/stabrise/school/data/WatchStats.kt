package com.stabrise.school.data

import android.content.Context
import android.content.SharedPreferences

/** How many videos were opened in each subject on this device (the API has no view counts). */
object WatchStats {
    /** A subject moves up only when it has this many more views than the one above it. */
    private const val MOVE_UP_LEAD = 25
    private const val ORDER_KEY = "order"

    private var prefs: SharedPreferences? = null

    /** Call before use so the counts are kept between app starts. */
    fun init(context: Context) {
        prefs = context.getSharedPreferences("watch_stats", Context.MODE_PRIVATE)
    }

    fun count(subjectId: Int): Int = prefs?.getInt(subjectId.toString(), 0) ?: 0

    /** The saved order of the subjects, updated by the views since then. New subjects go last. */
    fun order(subjects: List<Subject>): List<Subject> {
        if (subjects.isEmpty()) return subjects
        val saved = prefs?.getString(ORDER_KEY, null).orEmpty()
            .split(',').mapNotNull { it.toIntOrNull() }
        val position = saved.withIndex().associate { (index, id) -> id to index }
        val ordered = subjects.sortedBy { position[it.id] ?: Int.MAX_VALUE }.toMutableList()
        do {
            var moved = false
            for (i in 1 until ordered.size) {
                if (count(ordered[i].id) - count(ordered[i - 1].id) >= MOVE_UP_LEAD) {
                    ordered[i] = ordered[i - 1].also { ordered[i - 1] = ordered[i] }
                    moved = true
                }
            }
        } while (moved)
        prefs?.edit()?.putString(ORDER_KEY, ordered.joinToString(",") { it.id.toString() })?.apply()
        return ordered
    }

    fun add(subjectId: Int) {
        prefs?.edit()?.putInt(subjectId.toString(), count(subjectId) + 1)?.apply()
    }
}
