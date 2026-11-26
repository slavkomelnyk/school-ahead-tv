package com.stabrise.school.ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stabrise.school.data.FROM_CACHE
import com.stabrise.school.data.Lesson
import com.stabrise.school.data.LessonTopic
import com.stabrise.school.data.NetworkModule
import com.stabrise.school.data.Subject
import com.stabrise.school.data.youTubeVideoId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

data class PlayingVideo(val videoId: String, val title: String)

class SchoolTvViewModel : ViewModel() {
    var subjects by mutableStateOf<List<Subject>>(emptyList())
        private set

    var selectedSubject by mutableStateOf<Subject?>(null)
        private set

    var topics by mutableStateOf<List<LessonTopic>>(emptyList())
        private set

    var selectedTopic by mutableStateOf<LessonTopic?>(null)
        private set

    var lessons by mutableStateOf<List<Lesson>>(emptyList())
        private set

    var lessonCount by mutableStateOf(0)
        private set

    /** Search text within the selected subject; empty means no search. */
    var searchQuery by mutableStateOf("")
        private set

    /** Kept here so the row position survives a visit to the player. */
    var lessonRowState by mutableStateOf(LazyListState())
        private set

    var lastOpenedLessonId: Int? = null
        private set

    var playingVideo by mutableStateOf<PlayingVideo?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    /** Short message about a lesson that could not be opened. */
    var notice by mutableStateOf<String?>(null)
        private set

    private var loadJob: Job? = null
    private var openJob: Job? = null

    init {
        loadSubjects()
    }

    fun loadSubjects() {
        // Not part of load(): choosing a subject must not cancel the refresh of this list.
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                cachedThenFresh({ NetworkModule.api.getSubjects(it) }) { list ->
                    subjects = list
                    if (selectedSubject == null) list.firstOrNull()?.let { selectSubject(it) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (subjects.isEmpty()) errorMessage = e.localizedMessage ?: "Failed to load subjects"
            } finally {
                if (selectedSubject == null) isLoading = false
            }
        }
    }

    fun selectSubject(subject: Subject) {
        selectedSubject = subject
        selectedTopic = null
        searchQuery = ""
        topics = emptyList()
        reloadSubject()
    }

    fun selectTopic(topic: LessonTopic?) {
        selectedTopic = topic
        reloadLessons()
    }

    fun search(query: String) {
        if (query.trim() == searchQuery) return
        searchQuery = query.trim()
        reloadLessons()
    }

    fun loadMore() {
        val subject = selectedSubject ?: return
        if (loadJob?.isActive == true || lessons.size >= lessonCount) return
        load {
            val page = NetworkModule.api.getLessonsPage(
                subjectId = subject.id,
                topicId = selectedTopic?.id,
                query = searchQuery.ifEmpty { null },
                offset = lessons.size
            )
            lessons = (lessons + page.items).distinctBy { it.id }
            lessonCount = page.count
        }
    }

    fun retry() {
        if (selectedSubject == null) loadSubjects() else reloadSubject()
    }

    fun openLesson(lesson: Lesson) {
        lastOpenedLessonId = lesson.id
        notice = null
        openJob?.cancel()
        openJob = viewModelScope.launch {
            try {
                val videoId = NetworkModule.api.getLessonDetails(lesson.id).youTubeVideoId()
                if (videoId == null) {
                    notice = "This lesson has no video"
                } else {
                    playingVideo = PlayingVideo(videoId, lesson.title)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                notice = "Could not open the lesson. Check the network and try again."
            }
        }
    }

    fun closePlayer() {
        playingVideo = null
    }

    /** Topics and lessons are requested at the same time. */
    private fun reloadSubject() {
        val subject = selectedSubject ?: return
        clearLessons()
        load {
            coroutineScope {
                if (topics.isEmpty()) {
                    launch {
                        cachedThenFresh({ NetworkModule.api.getLessonTopics(subject.id, it) }) { topics = it }
                    }
                }
                showFirstPage(subject.id)
            }
        }
    }

    private fun reloadLessons() {
        val subject = selectedSubject ?: return
        // A topic list that has not arrived yet is requested again.
        if (topics.isEmpty()) return reloadSubject()
        clearLessons()
        load { showFirstPage(subject.id) }
    }

    private suspend fun showFirstPage(subjectId: Int) {
        val topicId = selectedTopic?.id
        val query = searchQuery.ifEmpty { null }
        cachedThenFresh({
            NetworkModule.api.getLessonsPage(subjectId, topicId, query, cache = it)
        }) { page ->
            lessons = page.items
            lessonCount = page.count
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

    private fun clearLessons() {
        openJob?.cancel()
        lessons = emptyList()
        lessonCount = 0
        lessonRowState = LazyListState()
        lastOpenedLessonId = null
        notice = null
    }

    /** Only the latest request may update the screen. */
    private fun load(block: suspend () -> Unit) {
        loadJob?.cancel()
        isLoading = true
        errorMessage = null
        var job: Job? = null
        job = viewModelScope.launch(start = kotlinx.coroutines.CoroutineStart.LAZY) {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                errorMessage = e.localizedMessage ?: "Failed to load"
            } finally {
                if (loadJob === job) isLoading = false
            }
        }
        loadJob = job
        job.start()
    }
}
