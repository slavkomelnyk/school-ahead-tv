package com.stabrise.school.data

import com.google.gson.annotations.SerializedName

data class Subject(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("icon") val iconUrl: String? = null
)

data class LessonTopic(
    @SerializedName("id") val id: Int,
    @SerializedName("title") val title: String
)

data class LessonPageResponse(
    @SerializedName("items") val items: List<Lesson>,
    @SerializedName("count") val count: Int
)

data class Lesson(
    @SerializedName("id") val id: Int,
    @SerializedName("title") val title: String,
    @SerializedName("icon") val iconUrl: String? = null
)

data class LessonDetail(
    @SerializedName("id") val id: Int,
    @SerializedName("title") val title: String,
    @SerializedName("content") val content: String? = null,
    @SerializedName("task_content") val taskContent: String? = null,
    @SerializedName("materials") val materials: List<LessonMaterial>? = null
)

data class LessonMaterial(
    @SerializedName("url") val url: String? = null,
    @SerializedName("kind") val kind: String? = null
)

private val youTubeLink = Regex(
    """(?:youtube(?:-nocookie)?\.com/(?:watch\?(?:[^\s"'<>)]*&)?v=|embed/|shorts/|live/)|youtu\.be/)([A-Za-z0-9_-]{11})"""
)

/** The lesson API has no video field: the YouTube link is in the text or the attachments. */
fun LessonDetail.youTubeVideoId(): String? {
    val sources = listOf(content) + materials.orEmpty().map { it.url } + taskContent
    return sources.firstNotNullOfOrNull { text ->
        text?.let { youTubeLink.find(it)?.groupValues?.get(1) }
    }
}
