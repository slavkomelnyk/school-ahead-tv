@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package com.stabrise.school

import android.os.Bundle
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.ExperimentalFoundationApi
import coil.ImageLoader
import coil.Coil
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.foundation.focusGroup
import java.util.Date
import kotlinx.coroutines.delay
import com.stabrise.school.data.UnfinishedVideo
import com.stabrise.school.data.ContinueWatching
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import android.text.format.DateFormat
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.*
import coil.compose.AsyncImage
import com.stabrise.school.data.Lesson
import com.stabrise.school.data.NetworkModule
import com.stabrise.school.data.Subject
import com.stabrise.school.data.WatchStats
import com.stabrise.school.ui.RowKey
import com.stabrise.school.ui.SchoolTvViewModel
import com.stabrise.school.ui.VideoPlayer
import com.stabrise.school.ui.theme.SchoolTheme

private const val CARDS_PER_ROW = 3
private val CARD_GAP = 16.dp
private val PAGE_START = 20.dp

/** Set from the screen width so that exactly [CARDS_PER_ROW] cards fit in a row. */
private val LocalCardWidth = compositionLocalOf { 240.dp }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Keep the screen awake: no screensaver or sleep while the app is on screen.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        NetworkModule.init(applicationContext)
        // Pictures fade in and stay saved, whatever the cache headers of the server say.
        Coil.setImageLoader(
            ImageLoader.Builder(applicationContext)
                .crossfade(true)
                .respectCacheHeaders(false)
                .build()
        )
        WatchStats.init(applicationContext)
        ContinueWatching.init(applicationContext)
        setContent {
            SchoolTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RectangleShape
                ) {
                    SchoolTvApp()
                }
            }
        }
    }
}

@Composable
fun SchoolTvApp(viewModel: SchoolTvViewModel = viewModel()) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    viewModel.playingVideo?.video?.let { video ->
        VideoPlayer(
            videoId = video.videoId,
            title = video.title,
            startSeconds = video.position.toInt(),
            onProgress = viewModel::onProgress,
            onClose = { viewModel.closePlayer() }
        )
        return
    }

    BackHandler(enabled = viewModel.selectedSubject != null && drawerState.currentValue == DrawerValue.Closed) {
        viewModel.goHome()
    }
    BackHandler(enabled = drawerState.currentValue == DrawerValue.Open) {
        drawerState.setValue(DrawerValue.Closed)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = { drawerValue ->
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(12.dp)
                    // The drawer opens as soon as it has focus. When the focused item of a page
                    // disappears, focus must not fall in here: only a move to the left enters.
                    .focusProperties {
                        onEnter = {
                            if (requestedFocusDirection != FocusDirection.Left) cancelFocusChange()
                        }
                    }
                    .focusGroup(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // The closed drawer is only wide enough for icons.
                if (drawerValue == DrawerValue.Open) {
                    Column {
                        Text(
                            text = "School Ahead",
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Version: ${BuildConfig.VERSION_NAME}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                NavigationDrawerItem(
                    selected = viewModel.selectedSubject == null,
                    onClick = {
                        viewModel.goHome()
                        drawerState.setValue(DrawerValue.Closed)
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "Home"
                        )
                    }
                ) {
                    Text(
                        text = "Home",
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

            }
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 80.dp)
        ) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                // A gap before, between and after the cards, so a fourth one stays off screen.
                val cardWidth = (maxWidth - PAGE_START - CARD_GAP * (CARDS_PER_ROW + 1)) / CARDS_PER_ROW
                CompositionLocalProvider(LocalCardWidth provides cardWidth) {
                    val nothingToShow = if (viewModel.selectedSubject == null) viewModel.subjects.isEmpty()
                    else viewModel.topics == null
                    if (viewModel.errorMessage != null && nothingToShow) {
                        ErrorStateView(
                            message = viewModel.errorMessage ?: "Network error",
                            onRetry = { viewModel.retry() }
                        )
                    } else if (viewModel.selectedSubject == null) {
                        HomeScreen(viewModel)
                    } else {
                        SubjectScreen(viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun HomeScreen(viewModel: SchoolTvViewModel) {
    // Most watched first, but without jumping after every video: see WatchStats.order.
    val subjects = remember(viewModel.subjects) { WatchStats.order(viewModel.subjects) }
    // The row that takes focus: the last visited one, otherwise the first one.
    val targetIndex = subjects.indexOfFirst { it.id == viewModel.lastSubjectId }.coerceAtLeast(0)
    val target = subjects.getOrNull(targetIndex)
    val targetLessonId = viewModel.homeFocus?.takeIf { it.row.subjectId == target?.id }?.lessonId
    val continueVideo = viewModel.continueVideo
    val onHeader = viewModel.homeHeaderFocused
    val headerTakesFocus = onHeader && continueVideo != null
    // The header is the first item of the list.
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = if (onHeader) 0 else targetIndex + 1
    )
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(target?.id) {
        if (target != null) {
            withFrameNanos { }
            runCatching { focusRequester.requestFocus() }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = PAGE_START, top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PageStatus(
            viewModel.notice ?: viewModel.errorMessage ?: when {
                subjects.isNotEmpty() -> null
                viewModel.isLoading -> "Loading..."
                else -> "No subjects available"
            }
        )
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            item(key = "header") {
                HomeHeader(
                    video = continueVideo,
                    buttonModifier = if (headerTakesFocus) Modifier.focusRequester(focusRequester) else Modifier,
                    onContinue = { viewModel.continueWatching() }
                )
            }
            items(subjects, key = { it.id }) { subject ->
                val isTarget = !headerTakesFocus && subject.id == target?.id
                LessonRow(
                    viewModel = viewModel,
                    key = RowKey(subject.id),
                    focusLessonId = targetLessonId.takeIf { isTarget },
                    focusRequester = focusRequester.takeIf { isTarget },
                    // The subject takes the place of the first video and opens all its topics.
                    firstItem = { modifier ->
                        SubjectCard(subject, modifier) { viewModel.selectSubject(subject) }
                    }
                )
            }
        }
    }
}

/** App name and clock, then the last unfinished video if there is one. */
@Composable
private fun HomeHeader(video: UnfinishedVideo?, buttonModifier: Modifier, onContinue: () -> Unit) {
    val context = LocalContext.current
    val clock by produceState("") {
        while (true) {
            value = DateFormat.getTimeFormat(context).format(Date())
            delay(1000)
        }
    }

    Column(
        modifier = Modifier.padding(start = CARD_GAP, end = 48.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "School Ahead",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f)
            )
            Text(text = clock, style = MaterialTheme.typography.titleMedium)
        }
        if (video != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(240.dp)
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    AsyncImage(
                        model = video.iconUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    // How much of the video has been watched.
                    val watched = if (video.duration > 0) video.position / video.duration else 0f
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth(watched.coerceIn(0f, 1f))
                            .height(4.dp)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Continue watching",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Button(onClick = onContinue, modifier = buttonModifier) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Continue")
                    }
                }
            }
        }
    }
}

/**
 * Subject icon, name, focused video title and search on the left, the picture of the
 * focused video on the right.
 */
@Composable
private fun SubjectBanner(subject: Subject, focused: () -> Lesson?, search: @Composable () -> Unit) {
    val lesson = focused()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(190.dp)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .aspectRatio(16f / 9f)
        ) {
            AsyncImage(
                model = lesson?.iconUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Fades the picture into the page.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            0f to MaterialTheme.colorScheme.surface,
                            0.5f to Color.Transparent
                        )
                    )
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = CARD_GAP)
                .fillMaxWidth(0.5f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AsyncImage(
                    model = subject.iconUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(40.dp).clip(CircleShape)
                )
                Text(
                    text = subject.name,
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = lesson?.title.orEmpty(),
                style = MaterialTheme.typography.titleMedium,
                minLines = 2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            search()
        }
    }
}

/** A subject page: a row of videos for every topic, or the search results. */
@Composable
fun SubjectScreen(viewModel: SchoolTvViewModel) {
    val subject = viewModel.selectedSubject ?: return
    val query = viewModel.searchQuery
    val topics = viewModel.topics
    // Row title and content. A subject without topics gets one row with all its lessons.
    val rows: List<Pair<String, RowKey>> = when {
        query.isNotEmpty() -> listOf("Search results" to RowKey(subject.id, query = query))
        topics == null -> emptyList()
        topics.isEmpty() -> listOf("All lessons" to RowKey(subject.id))
        else -> topics.map { it.title to RowKey(subject.id, topicId = it.id) }
    }
    val focus = viewModel.subjectFocus
    val target = focus?.row ?: rows.firstOrNull()?.second
    val targetLessons = target?.let { viewModel.row(it).lessons }
    val targetLessonId = focus?.lessonId ?: targetLessons?.firstOrNull()?.id
    val focusRequester = remember { FocusRequester() }
    val clearFocusRequester = remember { FocusRequester() }
    var focusedLesson by remember(subject.id, query) { mutableStateOf<Lesson?>(null) }

    // Focus a video once the target row has loaded. With no search results, focus "Clear search".
    LaunchedEffect(target, targetLessons != null) {
        if (targetLessons != null) {
            withFrameNanos { }
            runCatching {
                if (targetLessons.isNotEmpty()) focusRequester.requestFocus()
                else if (query.isNotEmpty()) clearFocusRequester.requestFocus()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = PAGE_START, top = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SubjectBanner(subject, { focusedLesson ?: targetLessons?.find { it.id == targetLessonId } }) {
            SearchBar(
                subjectId = subject.id,
                query = query,
                clearFocusRequester = clearFocusRequester,
                onSearch = { viewModel.search(it) }
            )
        }
        PageStatus(
            viewModel.notice ?: viewModel.errorMessage
                ?: if (topics == null && query.isEmpty()) "Loading..." else null
        )
        if (query.isNotEmpty()) {
            LessonGrid(
                viewModel = viewModel,
                key = RowKey(subject.id, query = query),
                focusLessonId = targetLessonId,
                focusRequester = focusRequester,
                onLessonFocused = { focusedLesson = it }
            )
        } else LazyColumn(
            state = viewModel.subjectListState,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            items(rows, key = { (_, key) -> key.toString() }) { (title, key) ->
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(start = CARD_GAP)
                    )
                    LessonRow(
                        viewModel = viewModel,
                        key = key,
                        focusLessonId = targetLessonId.takeIf { key == target },
                        focusRequester = focusRequester.takeIf { key == target },
                        onLessonFocused = { focusedLesson = it }
                    )
                }
            }
        }
    }
}

@Composable
private fun PageStatus(text: String?) {
    if (text == null) return
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = CARD_GAP)
    )
}

/**
 * A horizontal row of lessons that loads more near its end. Focus goes to the lesson
 * [focusLessonId], or to [firstItem] when there is no such lesson.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LessonRow(
    viewModel: SchoolTvViewModel,
    key: RowKey,
    focusLessonId: Int?,
    focusRequester: FocusRequester?,
    onLessonFocused: (Lesson) -> Unit = {},
    firstItem: (@Composable (Modifier) -> Unit)? = null
) {
    val row = viewModel.row(key)
    val lessons = row.lessons
    val focus = if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier
    val hasTargetLesson = lessons?.any { it.id == focusLessonId } == true

    LaunchedEffect(key) { viewModel.loadRow(key) }
    // Load the next page when the row is scrolled near its end, by touch or mouse.
    LaunchedEffect(key, lessons?.size) {
        snapshotFlow {
            val layout = row.listState.layoutInfo
            layout.totalItemsCount - (layout.visibleItemsInfo.lastOrNull()?.index ?: -1)
        }.collect { if (it <= 5) viewModel.loadMore(key) }
    }

    CompositionLocalProvider(LocalBringIntoViewSpec provides rememberStillRowSpec()) {
        LazyRow(
            state = row.listState,
            horizontalArrangement = Arrangement.spacedBy(CARD_GAP),
            // Room for the focused card to grow without being cut off.
            contentPadding = PaddingValues(horizontal = CARD_GAP)
        ) {
            if (firstItem != null) {
                item(key = "first") { firstItem(if (hasTargetLesson) Modifier else focus) }
            }
            if (lessons.isNullOrEmpty()) {
                item(key = "status") {
                    if (row.failed) {
                        Button(onClick = { viewModel.loadRow(key) }, modifier = Modifier.padding(top = 48.dp)) {
                            Text("Could not load. Retry")
                        }
                    } else if (lessons == null) {
                        LoadingCards()
                    } else {
                        Text(
                            text = "No lessons available",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            // As tall as a video, so the list does not jump when videos arrive.
                            modifier = Modifier.width(LocalCardWidth.current).padding(top = 52.dp, bottom = 100.dp)
                        )
                    }
                }
            } else {
                itemsIndexed(
                    lessons,
                    key = { _, lesson -> lesson.id },
                    contentType = { _, _ -> "lesson" }
                ) { index, lesson ->
                    LessonCard(
                        lesson = lesson,
                        modifier = if (lesson.id == focusLessonId) focus else Modifier,
                        // The remote moves focus; this also retries a page that failed to load.
                        onFocused = {
                            onLessonFocused(lesson)
                            if (index >= lessons.size - 5) viewModel.loadMore(key)
                        },
                        onClick = { viewModel.openLesson(lesson, key) }
                    )
                }
            }
        }
    }
}

/**
 * A row scrolls only when the focused card is not fully visible, and then just far enough.
 * So moving up or down lands on the card at the same place and the row stays where it was.
 * (By default a TV moves every focused card to a fixed place in its row.)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun rememberStillRowSpec(): BringIntoViewSpec {
    // The space left beside a card, so that it can grow when focused.
    val margin = with(LocalDensity.current) { CARD_GAP.toPx() }
    return remember(margin) {
        object : BringIntoViewSpec {
            override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
                val before = offset - margin
                val after = offset + size + margin - containerSize
                return when {
                    before < 0f -> before
                    after > 0f -> after
                    else -> 0f
                }
            }
        }
    }
}

/** All videos of a search, as a grid that loads more near its end. */
@Composable
private fun LessonGrid(
    viewModel: SchoolTvViewModel,
    key: RowKey,
    focusLessonId: Int?,
    focusRequester: FocusRequester,
    onLessonFocused: (Lesson) -> Unit
) {
    val row = viewModel.row(key)
    val lessons = row.lessons

    LaunchedEffect(key) { viewModel.loadRow(key) }

    when {
        row.failed -> Button(onClick = { viewModel.loadRow(key) }, modifier = Modifier.padding(12.dp)) {
            Text("Could not load. Retry")
        }
        lessons == null -> Box(Modifier.padding(12.dp)) { LoadingCards() }
        lessons.isEmpty() -> PageStatus("Nothing found")
        else -> LazyVerticalGrid(
            columns = GridCells.Fixed(CARDS_PER_ROW),
            horizontalArrangement = Arrangement.spacedBy(CARD_GAP),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(start = CARD_GAP, top = 12.dp, end = CARD_GAP, bottom = 32.dp)
        ) {
            gridItemsIndexed(lessons, key = { _, lesson -> lesson.id }) { index, lesson ->
                LessonCard(
                    lesson = lesson,
                    modifier = (if (lesson.id == focusLessonId) Modifier.focusRequester(focusRequester)
                    else Modifier).fillMaxWidth(),
                    onFocused = {
                        onLessonFocused(lesson)
                        if (index >= lessons.size - 8) viewModel.loadMore(key)
                    },
                    onClick = { viewModel.openLesson(lesson, key) }
                )
            }
        }
    }
}

/** Grey shapes in place of videos that are still loading. */
@Composable
private fun LoadingCards() {
    Row(horizontalArrangement = Arrangement.spacedBy(CARD_GAP)) {
        repeat(CARDS_PER_ROW) {
            Column(
                // As tall as a video with its title, so the list does not jump when videos arrive.
                modifier = Modifier.width(LocalCardWidth.current).padding(bottom = 30.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val color = MaterialTheme.colorScheme.surfaceVariant
                Box(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .background(color, RoundedCornerShape(12.dp))
                )
                Box(Modifier.fillMaxWidth(0.7f).height(12.dp).background(color, RoundedCornerShape(6.dp)))
            }
        }
    }
}

/** Icon and name of a subject, the same size as a video thumbnail. */
@Composable
private fun SubjectCard(subject: Subject, modifier: Modifier, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = modifier
            .width(LocalCardWidth.current)
            .aspectRatio(16f / 9f),
        shape = CardDefaults.shape(RoundedCornerShape(12.dp)),
        border = CardDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface),
                shape = RoundedCornerShape(12.dp)
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                )
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
        ) {
            AsyncImage(
                model = subject.iconUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(64.dp).clip(CircleShape)
            )
            Text(
                text = subject.name,
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Search field on the page. It is a plain button until pressed, so moving the remote over it
 * (or the app giving it focus) never opens the keyboard.
 */
@Composable
fun SearchBar(
    subjectId: Int?,
    query: String,
    clearFocusRequester: FocusRequester,
    onSearch: (String) -> Unit
) {
    var text by remember(subjectId, query) { mutableStateOf(query) }
    var editing by remember(subjectId) { mutableStateOf(false) }
    val fieldFocusRequester = remember { FocusRequester() }
    val buttonFocusRequester = remember { FocusRequester() }
    // Set when the field is closed by a key, so focus goes back to the button instead of getting lost.
    var restoreFocus by remember(subjectId) { mutableStateOf(false) }
    val keyboard = LocalSoftwareKeyboardController.current
    val shape = RoundedCornerShape(24.dp)
    val colors = MaterialTheme.colorScheme

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (editing) {
            var hadFocus by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                withFrameNanos { }
                runCatching { fieldFocusRequester.requestFocus() }
                keyboard?.show()
            }
            BackHandler {
                text = query
                restoreFocus = true
                editing = false
            }
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface),
                cursorBrush = SolidColor(colors.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        keyboard?.hide()
                        // Focus stays here until the results of a new search take it.
                        restoreFocus = true
                        editing = false
                        onSearch(text)
                    }
                ),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(fieldFocusRequester)
                    .onFocusChanged {
                        if (it.isFocused) hadFocus = true else if (hadFocus) editing = false
                    },
                decorationBox = { field ->
                    Box(
                        modifier = Modifier
                            .background(colors.surface, shape)
                            .border(3.dp, colors.primary, shape)
                    ) {
                        SearchBarContent { field() }
                    }
                }
            )
        } else {
            LaunchedEffect(Unit) {
                if (restoreFocus) {
                    restoreFocus = false
                    withFrameNanos { }
                    runCatching { buttonFocusRequester.requestFocus() }
                }
            }
            Surface(
                onClick = { editing = true },
                modifier = Modifier.weight(1f).focusRequester(buttonFocusRequester),
                shape = ClickableSurfaceDefaults.shape(shape),
                scale = ClickableSurfaceDefaults.scale(focusedScale = 1.03f),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = colors.surfaceVariant,
                    focusedContainerColor = colors.inverseSurface
                )
            ) {
                SearchBarContent {
                    Text(
                        text = text.ifEmpty { "Search in this subject" },
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        if (query.isNotEmpty()) {
            Button(
                onClick = {
                    text = ""
                    runCatching { buttonFocusRequester.requestFocus() }
                    onSearch("")
                },
                modifier = Modifier.focusRequester(clearFocusRequester)
            ) {
                Text("Clear search")
            }
        }
    }
}

@Composable
private fun SearchBarContent(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(Icons.Default.Search, contentDescription = null, Modifier.size(20.dp))
        content()
    }
}

@Composable
fun LessonCard(
    lesson: Lesson,
    modifier: Modifier = Modifier,
    onFocused: () -> Unit,
    onClick: () -> Unit
) {
    StandardCardContainer(
        // A width set by the caller (the grid) wins over the width of a row card.
        modifier = modifier.width(LocalCardWidth.current),
        imageCard = { interactionSource ->
            Card(
                onClick = onClick,
                interactionSource = interactionSource,
                shape = CardDefaults.shape(RoundedCornerShape(12.dp)),
                border = CardDefaults.border(
                    focusedBorder = Border(
                        border = BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface),
                        shape = RoundedCornerShape(12.dp)
                    )
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .onFocusChanged { if (it.isFocused) onFocused() }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    AsyncImage(
                        model = lesson.iconUrl,
                        contentDescription = lesson.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        },
        title = {
            Text(
                text = lesson.title,
                style = MaterialTheme.typography.titleSmall,
                minLines = 2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 10.dp)
            )
        }
    )
}

@Composable
fun ErrorStateView(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Could not load content",
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onRetry) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Retry")
            }
        }
    }
}