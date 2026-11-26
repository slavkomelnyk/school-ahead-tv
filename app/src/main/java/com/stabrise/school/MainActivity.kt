@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package com.stabrise.school

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.*
import coil.compose.AsyncImage
import com.stabrise.school.data.Lesson
import com.stabrise.school.data.NetworkModule
import com.stabrise.school.ui.SchoolTvViewModel
import com.stabrise.school.ui.VideoPlayer
import com.stabrise.school.ui.theme.SchoolTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Keep the screen awake: no screensaver or sleep while the app is on screen.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        NetworkModule.init(applicationContext)
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

    viewModel.playingVideo?.let { video ->
        VideoPlayer(videoId = video.videoId, title = video.title, onClose = { viewModel.closePlayer() })
        return
    }

    BackHandler(enabled = drawerState.currentValue == DrawerValue.Open) {
        drawerState.setValue(DrawerValue.Closed)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NavigationDrawerItem(
                    selected = false,
                    onClick = {},
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "Stabrise School"
                        )
                    }
                ) {
                    Text(
                        text = "Stabrise School",
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(viewModel.subjects, key = { it.id }) { subject ->
                        NavigationDrawerItem(
                            selected = viewModel.selectedSubject?.id == subject.id,
                            onClick = {
                                viewModel.selectSubject(subject)
                                drawerState.setValue(DrawerValue.Closed)
                            },
                            leadingContent = {
                                AsyncImage(
                                    model = subject.iconUrl,
                                    contentDescription = subject.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(24.dp).clip(CircleShape)
                                )
                            }
                        ) {
                            Text(
                                text = subject.name,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 80.dp)
        ) {
            if (viewModel.errorMessage != null && viewModel.lessons.isEmpty()) {
                ErrorStateView(
                    message = viewModel.errorMessage ?: "Network error",
                    onRetry = { viewModel.retry() }
                )
            } else {
                ImmersiveLessonScreen(viewModel)
            }
        }
    }
}

@Composable
fun ImmersiveLessonScreen(viewModel: SchoolTvViewModel) {
    val topics = viewModel.topics
    val selectedTopic = viewModel.selectedTopic
    val lessons = viewModel.lessons
    val firstId = lessons.firstOrNull()?.id
    var focusedLesson by remember(firstId) {
        mutableStateOf(lessons.find { it.id == viewModel.lastOpenedLessonId } ?: lessons.firstOrNull())
    }
    // The card that takes focus: the last opened lesson after playback, otherwise the first one.
    val targetId = remember(firstId) { viewModel.lastOpenedLessonId ?: firstId }
    val cardFocusRequester = remember { FocusRequester() }
    val clearFocusRequester = remember { FocusRequester() }
    val query = viewModel.searchQuery

    // With no results there is no card to focus, so focus "Clear search".
    LaunchedEffect(query, viewModel.isLoading) {
        if (query.isNotEmpty() && !viewModel.isLoading && viewModel.lessons.isEmpty()) {
            withFrameNanos { }
            runCatching { clearFocusRequester.requestFocus() }
        }
    }

    // Load the next page when the row is scrolled near its end, by remote, touch or mouse.
    val rowState = viewModel.lessonRowState
    LaunchedEffect(rowState) {
        snapshotFlow {
            Triple(
                rowState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1,
                viewModel.lessons.size,
                !viewModel.isLoading && viewModel.errorMessage == null
            )
        }.collect { (lastVisible, size, _) ->
            if (size > 0 && lastVisible >= size - 5) viewModel.loadMore()
        }
    }

    LaunchedEffect(firstId) {
        if (firstId != null) {
            withFrameNanos { }
            runCatching { cardFocusRequester.requestFocus() }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        focusedLesson?.let { lesson ->
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .aspectRatio(16f / 9f)
                    .align(Alignment.TopEnd)
            ) {
                AsyncImage(
                    model = lesson.iconUrl,
                    contentDescription = lesson.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(MaterialTheme.colorScheme.background, Color.Transparent),
                                startX = 0f,
                                endX = 650f
                            )
                        )
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, MaterialTheme.colorScheme.background),
                                startY = 180f
                            )
                        )
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 32.dp, top = 36.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(0.50f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SearchBar(
                    subjectId = viewModel.selectedSubject?.id,
                    query = query,
                    clearFocusRequester = clearFocusRequester,
                    onSearch = { viewModel.search(it) }
                )
                Text(
                    text = (viewModel.selectedSubject?.name ?: "Lessons").uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = focusedLesson?.title ?: if (viewModel.isLoading) "Loading..." else if (query.isNotEmpty()) "Nothing found" else "No lessons available",
                    style = MaterialTheme.typography.headlineLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = viewModel.notice ?: viewModel.errorMessage ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                if (viewModel.selectedSubject != null) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(end = 48.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = selectedTopic == null,
                                onClick = { viewModel.selectTopic(null) }
                            ) {
                                Text("All Topics")
                            }
                        }
                        items(topics, key = { it.id }) { topic ->
                            FilterChip(
                                selected = selectedTopic?.id == topic.id,
                                onClick = { viewModel.selectTopic(topic) }
                            ) {
                                Text(topic.title)
                            }
                        }
                    }
                }
                Text(
                    text = if (viewModel.isLoading && lessons.isEmpty()) "Loading..." else "Lessons (${viewModel.lessonCount})",
                    style = MaterialTheme.typography.titleMedium
                )
                LazyRow(
                    state = viewModel.lessonRowState,
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    contentPadding = PaddingValues(end = 48.dp)
                ) {
                    itemsIndexed(lessons, key = { _, lesson -> lesson.id }) { index, lesson ->
                        LessonCard(
                            lesson = lesson,
                            modifier = if (lesson.id == targetId) Modifier.focusRequester(cardFocusRequester) else Modifier,
                            onFocused = {
                                focusedLesson = lesson
                                // Also retries a page that failed to load.
                                if (index >= lessons.size - 5) viewModel.loadMore()
                            },
                            onClick = { viewModel.openLesson(lesson) }
                        )
                    }
                    if (viewModel.isLoading && lessons.isNotEmpty() && lessons.size < viewModel.lessonCount) {
                        item {
                            Text(
                                text = "Loading more...",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 48.dp)
                            )
                        }
                    }
                }
            }
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
            }
            BackHandler { editing = false }
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface),
                cursorBrush = SolidColor(colors.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
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
            Surface(
                onClick = { editing = true },
                modifier = Modifier.weight(1f),
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
        modifier = modifier.width(220.dp),
        imageCard = { interactionSource ->
            Card(
                onClick = onClick,
                interactionSource = interactionSource,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .onFocusChanged { if (it.isFocused) onFocused() }
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
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
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp)
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