package com.stabrise.school.ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stabrise.school.data.ContinueWatching
import com.stabrise.school.data.FROM_CACHE
import com.stabrise.school.data.Lesson
import com.stabrise.school.data.LessonTopic
import com.stabrise.school.data.NetworkModule
import com.stabrise.school.data.Subject
import com.stabrise.school.data.UnfinishedVideo
import com.stabrise.school.data.WatchStats
import com.stabrise.school.data.youTubeVideoId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs

/** The video in the player; [video] holds the position it starts from. */
data class PlayingVideo(val video: UnfinishedVideo)

/** Which lessons a row shows: a whole subject, one of its topics, or a search in it. */
data class RowKey(val subjectId: Int, val topicId: Int? = null, val query: String? = null)

/** The video to focus when a page is shown again. */
data class FocusTarget(val row: RowKey, val lessonId: Int?)

/** One horizontal row of lessons, loaded page by page. */
class LessonRowData {
    /** Null until the first page has arrived. */
    var lessons by mutableStateOf<List<Lesson>?>(null)
    var failed by mutableStateOf(false)
    var count = 0
    var busy = false

    /** Kept here so the row position survives a visit to the player. */
    val listState = LazyListState()
}

class SchoolTvViewModel : ViewModel() {
    var subjects by mutableStateOf<List<Subject>>(emptyList())
        private set

    var selectedSubject by mutableStateOf<Subject?>(null)
        private set

    /** Topics of the selected subject; null until they have arrived. */
    var topics by mutableStateOf<List<LessonTopic>?>(null)
        private set

    /** Search text within the selected subject; empty means no search. */
    var searchQuery by mutableStateOf("")
        private set

    var playingVideo by mutableStateOf<PlayingVideo?>(null)
        private set

    /** The last video that was not watched to the end, shown in the home header. */
    var continueVideo by mutableStateOf(ContinueWatching.all().firstOrNull())
        private set

    /** The home header takes focus after a video was continued from it. */
    var homeHeaderFocused = true
        private set

    private var progress: UnfinishedVideo? = null
    private var savedPosition = 0f

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    /** Short message about a lesson that could not be opened. */
    var notice by mutableStateOf<String?>(null)
        private set

    /** Subject to focus on the home page. */
    var lastSubjectId: Int? = null
        private set

    var homeFocus: FocusTarget? = null
        private set

    var subjectFocus: FocusTarget? = null
        private set

    /** Vertical position of the subject page. */
    var subjectListState = LazyListState()
        private set

    private val rows = mutableMapOf<RowKey, LessonRowData>()
    private var topicsJob: Job? = null
    private var openJob: Job? = null

    init {
        loadSubjects()
    }

    fun loadSubjects() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                cachedThenFresh({ NetworkModule.api.getSubjects(it) }) { subjects = it }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (subjects.isEmpty()) errorMessage = e.localizedMessage ?: "Failed to load subjects"
            } finally {
                isLoading = false
            }
        }
    }

    fun selectSubject(subject: Subject) {
        openJob?.cancel()
        selectedSubject = subject
        lastSubjectId = subject.id
        homeHeaderFocused = false
        homeFocus = null
        subjectFocus = null
        subjectListState = LazyListState()
        searchQuery = ""
        topics = null
        notice = null
        loadTopics(subject.id)
    }

    fun goHome() {
        if (selectedSubject == null) return
        openJob?.cancel()
        topicsJob?.cancel()
        selectedSubject = null
        searchQuery = ""
        topics = null
        notice = null
        isLoading = false
        errorMessage = null
    }

    fun search(query: String) {
        if (query.trim() == searchQuery) return
        searchQuery = query.trim()
        subjectFocus = null
        subjectListState = LazyListState()
        notice = null
    }

    fun retry() {
        val subject = selectedSubject
        if (subject == null) loadSubjects() else loadTopics(subject.id)
    }

    fun row(key: RowKey): LessonRowData = rows.getOrPut(key) { LessonRowData() }

    /** Loads the first page of a row once. A failed row can be loaded again. */
    fun loadRow(key: RowKey) {
        val row = row(key)
        if (row.lessons != null || row.busy) return
        row.failed = false
        // No next page until the first one is final.
        row.busy = true
        viewModelScope.launch {
            try {
                cachedThenFresh({
                    NetworkModule.api.getLessonsPage(key.subjectId, key.topicId, key.query, cache = it)
                }) {
                    row.count = it.count
                    row.lessons = it.items
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (row.lessons == null) row.failed = true
            } finally {
                row.busy = false
            }
        }
    }

    /** Adds the next page to a row. */
    fun loadMore(key: RowKey) {
        val row = row(key)
        val loaded = row.lessons ?: return
        if (row.busy || loaded.size >= row.count) return
        row.busy = true
        viewModelScope.launch {
            try {
                val page = NetworkModule.api.getLessonsPage(
                    key.subjectId, key.topicId, key.query, offset = loaded.size
                )
                row.count = page.count
                row.lessons = (loaded + page.items).distinctBy { it.id }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // The row keeps what it has; moving along it asks again.
            } finally {
                row.busy = false
            }
        }
    }

    fun openLesson(lesson: Lesson, key: RowKey) {
        if (selectedSubject == null) {
            lastSubjectId = key.subjectId
            homeHeaderFocused = false
            homeFocus = FocusTarget(key, lesson.id)
        } else {
            subjectFocus = FocusTarget(key, lesson.id)
        }
        notice = null
        openJob?.cancel()
        openJob = viewModelScope.launch {
            try {
                val videoId = NetworkModule.api.getLessonDetails(lesson.id).youTubeVideoId()
                if (videoId == null) {
                    notice = "This lesson has no video"
                } else {
                    // An unfinished video goes on where it stopped.
                    val saved = ContinueWatching.all().find { it.lessonId == lesson.id && it.videoId == videoId }
                    play(
                        UnfinishedVideo(
                            lessonId = lesson.id,
                            subjectId = key.subjectId,
                            videoId = videoId,
                            title = lesson.title,
                            iconUrl = lesson.iconUrl,
                            position = saved?.position ?: 0f,
                            duration = saved?.duration ?: 0f
                        )
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                notice = "Could not open the lesson. Check the network and try again."
            }
        }
    }

    fun continueWatching() {
        val video = continueVideo ?: return
        openJob?.cancel()
        notice = null
        homeHeaderFocused = true
        play(video)
    }

    /** Position reported by the player; saved every few seconds and when the player closes. */
    fun onProgress(position: Float, duration: Float) {
        val video = playingVideo?.video ?: return
        if (position < 1f || duration <= 0f) return
        val updated = video.copy(position = position, duration = duration)
        progress = updated
        val finished = ContinueWatching.isFinished(position, duration)
        if (finished || abs(position - savedPosition) >= 5f) saveProgress()
    }

    fun closePlayer() {
        saveProgress()
        playingVideo = null
    }

    private fun play(video: UnfinishedVideo) {
        WatchStats.add(video.subjectId)
        progress = null
        savedPosition = video.position
        playingVideo = PlayingVideo(video)
    }

    private fun saveProgress() {
        val video = progress ?: return
        savedPosition = video.position
        ContinueWatching.save(video)
        continueVideo = ContinueWatching.all().firstOrNull()
    }

    private fun loadTopics(subjectId: Int) {
        topicsJob?.cancel()
        isLoading = true
        errorMessage = null
        topicsJob = viewModelScope.launch {
            try {
                cachedThenFresh({ NetworkModule.api.getLessonTopics(subjectId, it) }) { topics = it }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (topics == null) errorMessage = e.localizedMessage ?: "Failed to load topics"
            }
            isLoading = false
        }
    }

    /** Shows the saved answer at once (if there is one), then the one from the server. */
    private suspend fun <T> cachedThenFresh(
        request: suspend (cache: String?) -> T,
        show: (T) -> Unit
    ) {
        try {
            show(request(FROM_CACHE))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Nothing saved yet.
        }
        show(request(null))
    }
}
