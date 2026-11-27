@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package com.stabrise.school.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.IconButtonDefaults
import androidx.tv.material3.LocalContentColor
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import kotlinx.coroutines.delay
import org.json.JSONArray
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

private data class Playback(val state: Int = -1, val position: Float = 0f, val duration: Float = 0f) {
    val isPlaying get() = state == 1
    val isBuffering get() = state == 3 || state == -1
}

private const val CONTROLS_TIMEOUT_MS = 4000L

/**
 * Full screen YouTube embedded player with the app's own controls.
 * Remote, controls hidden: OK shows them with focus on play / pause, left / right seek.
 * Remote, controls shown: arrows move between the buttons; on the progress line left / right
 * seek. A click jumps 5 seconds, holding moves faster the longer it is held.
 * Back hides the controls, and closes the player when they are already hidden.
 * Touch or mouse: click the video to show or hide the controls.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun VideoPlayer(
    videoId: String,
    title: String,
    startSeconds: Int,
    onProgress: (position: Float, duration: Float) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }
    val playFocusRequester = remember { FocusRequester() }
    val barFocusRequester = remember { FocusRequester() }
    // YouTube rejects embeds that send no referrer, so the page gets the app id as its origin.
    val origin = "https://${context.packageName}"
    val webView = remember(videoId) {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(android.graphics.Color.BLACK)
            isFocusable = false
            isFocusableInTouchMode = false
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            webChromeClient = WebChromeClient()
            webViewClient = WebViewClient()
            loadDataWithBaseURL(origin, playerPage(videoId, origin, startSeconds), "text/html", "utf-8", null)
        }
    }

    var playback by remember(videoId) { mutableStateOf(Playback()) }
    var controlsVisible by remember { mutableStateOf(true) }
    // Changes on every user action, which restarts the hide timer.
    var interaction by remember { mutableIntStateOf(0) }

    // Where a seek is heading: shown at once, sent to YouTube when the key is released.
    var seekTarget by remember(videoId) { mutableStateOf<Float?>(null) }
    var seeking by remember { mutableStateOf(false) }
    var barFocused by remember { mutableStateOf(false) }
    // Set when seeking brought the controls up, so the next left / right keeps seeking.
    var focusBarOnShow by remember { mutableStateOf(false) }

    fun run(script: String) {
        controlsVisible = true
        interaction++
        webView.evaluateJavascript(script, null)
    }

    fun moveSeekTarget(forward: Boolean, repeatCount: Int) {
        if (playback.duration <= 0) return
        val step = seekStep(repeatCount)
        val from = seekTarget ?: playback.position
        seeking = true
        seekTarget = (from + if (forward) step else -step).coerceIn(0f, playback.duration)
        controlsVisible = true
        interaction++
    }

    fun finishSeek() {
        seeking = false
        seekTarget?.let { run("seekTo($it)") }
    }

    // Keep showing the target until the player has had time to report the new position.
    LaunchedEffect(seeking, seekTarget) {
        if (!seeking && seekTarget != null) {
            delay(1200)
            seekTarget = null
        }
    }

    LaunchedEffect(webView) {
        while (true) {
            webView.evaluateJavascript("playback()") { json ->
                runCatching {
                    val values = JSONArray(json)
                    playback = Playback(
                        state = values.getInt(0),
                        position = values.getDouble(1).toFloat(),
                        duration = values.getDouble(2).toFloat()
                    )
                    // Playing, paused or ended: before that the position is not real yet.
                    if (playback.state in 0..2) onProgress(playback.position, playback.duration)
                }
            }
            delay(500)
        }
    }

    // Controls stay up while the video is paused, loading or finished.
    LaunchedEffect(interaction, playback.isPlaying, controlsVisible, seeking) {
        if (controlsVisible && playback.isPlaying && !seeking) {
            delay(CONTROLS_TIMEOUT_MS)
            controlsVisible = false
        }
    }

    BackHandler {
        if (controlsVisible) controlsVisible = false else onClose()
    }
    // Focus goes to play / pause while the controls are up, otherwise to the video itself.
    LaunchedEffect(controlsVisible) {
        withFrameNanos { }
        runCatching {
            when {
                !controlsVisible -> focusRequester.requestFocus()
                focusBarOnShow -> barFocusRequester.requestFocus()
                else -> playFocusRequester.requestFocus()
            }
        }
        focusBarOnShow = false
        if (!controlsVisible) barFocused = false
    }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(webView, lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> webView.onPause()
                Lifecycle.Event.ON_RESUME -> webView.onResume()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            webView.destroy()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onPreviewKeyEvent { event ->
                val down = event.type == KeyEventType.KeyDown
                val firstDown = down && event.nativeKeyEvent.repeatCount == 0
                when (event.key) {
                    Key.DirectionLeft, Key.MediaRewind,
                    Key.DirectionRight, Key.MediaFastForward -> {
                        val forward =
                            event.key == Key.DirectionRight || event.key == Key.MediaFastForward
                        val isArrow =
                            event.key == Key.DirectionLeft || event.key == Key.DirectionRight
                        if (isArrow && controlsVisible && !barFocused && !seeking) {
                            // Let the arrow move focus between the buttons.
                            if (down) interaction++
                            false
                        } else {
                            if (down) {
                                if (!controlsVisible) focusBarOnShow = true
                                moveSeekTarget(forward, event.nativeKeyEvent.repeatCount)
                            } else if (seeking) {
                                finishSeek()
                            }
                            true
                        }
                    }
                    Key.MediaPlayPause -> { if (firstDown) run("toggle()"); true }
                    Key.MediaPlay -> { if (firstDown) run("play()"); true }
                    Key.MediaPause -> { if (firstDown) run("pause()"); true }
                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter,
                    Key.DirectionUp, Key.DirectionDown -> {
                        if (controlsVisible) {
                            // The focused control handles it; any press restarts the hide timer.
                            if (down) interaction++
                            false
                        } else {
                            // Shown on release, so this press does not also click the button.
                            if (!down) {
                                controlsVisible = true
                                interaction++
                            }
                            true
                        }
                    }
                    else -> false
                }
            }
            .focusRequester(focusRequester)
            .focusable()
    ) {
        AndroidView(factory = { webView }, modifier = Modifier.fillMaxSize())

        // Takes all touches, so YouTube's own interface never reacts to them.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures {
                        controlsVisible = !controlsVisible
                        interaction++
                    }
                }
        )

        if (controlsVisible) {
            PlayerControls(
                title = title,
                playback = playback,
                seekTarget = seekTarget,
                playFocusRequester = playFocusRequester,
                barFocusRequester = barFocusRequester,
                barFocused = barFocused,
                onBarFocus = { barFocused = it },
                onToggle = { run("toggle()") },
                onSkip = { forward ->
                    moveSeekTarget(forward, 0)
                    finishSeek()
                },
                onClose = onClose,
                onSeekToFraction = { run("seekTo(${it * playback.duration})") },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun PlayerControls(
    title: String,
    playback: Playback,
    seekTarget: Float?,
    playFocusRequester: FocusRequester,
    barFocusRequester: FocusRequester,
    barFocused: Boolean,
    onBarFocus: (Boolean) -> Unit,
    onToggle: () -> Unit,
    onSkip: (forward: Boolean) -> Unit,
    onClose: () -> Unit,
    onSeekToFraction: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val position = seekTarget ?: playback.position
    val progress =
        if (playback.duration > 0) (position / playback.duration).coerceIn(0f, 1f) else 0f
    val accent = MaterialTheme.colorScheme.primary
    val timeStyle = MaterialTheme.typography.labelSmall
    val status = when {
        seekTarget != null -> {
            val change = (seekTarget - playback.position).toInt()
            if (change == 0) "" else "%+d s".format(change)
        }
        playback.isBuffering -> "Loading..."
        else -> ""
    }

    // A small floating panel, so most of the picture stays visible.
    Column(
        modifier = modifier
            .padding(bottom = 20.dp)
            .widthIn(max = 560.dp)
            .fillMaxWidth(0.9f)
            .background(Color.Black.copy(alpha = 0.72f), RoundedCornerShape(16.dp))
            // Clicks on the panel itself must not hide it.
            .pointerInput(Unit) { detectTapGestures { } }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(status, style = timeStyle, color = Color.White)
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(formatTime(position), style = timeStyle, color = Color.White)
            // Progress line with a dot; it grows when the remote focus is on it.
            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .height(18.dp)
                    .focusRequester(barFocusRequester)
                    .onFocusChanged { onBarFocus(it.isFocused) }
                    .onKeyEvent { event ->
                        val ok = event.key == Key.DirectionCenter || event.key == Key.Enter
                        if (ok && event.type == KeyEventType.KeyUp) onToggle()
                        ok
                    }
                    .focusable()
                    .pointerInput(playback.duration) {
                        detectTapGestures { onSeekToFraction((it.x / size.width).coerceIn(0f, 1f)) }
                    }
            ) {
                val line = (if (barFocused) 5.dp else 3.dp).toPx()
                val y = size.height / 2
                val x = size.width * progress
                drawLine(Color.White.copy(alpha = 0.3f), Offset(0f, y), Offset(size.width, y), line, StrokeCap.Round)
                drawLine(accent, Offset(0f, y), Offset(x, y), line, StrokeCap.Round)
                drawCircle(
                    color = if (barFocused) Color.White else accent,
                    radius = (if (barFocused) 7.dp else 4.dp).toPx(),
                    center = Offset(x, y)
                )
            }
            Text(formatTime(playback.duration), style = timeStyle, color = Color.White)
        }
        Row(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ControlButton(onClick = { onSkip(false) }, size = 32.dp) {
                Text("−", style = MaterialTheme.typography.titleMedium)
            }
            ControlButton(
                onClick = onToggle,
                size = 40.dp,
                modifier = Modifier.focusRequester(playFocusRequester)
            ) {
                when {
                    playback.isPlaying -> PauseIcon()
                    playback.state == 0 -> Icon(Icons.Default.Refresh, "Replay", Modifier.size(20.dp))
                    else -> Icon(Icons.Default.PlayArrow, "Play", Modifier.size(22.dp))
                }
            }
            ControlButton(onClick = { onSkip(true) }, size = 32.dp) {
                Text("+", style = MaterialTheme.typography.titleMedium)
            }
            ControlButton(onClick = onClose, size = 32.dp) {
                Icon(Icons.Default.Close, contentDescription = "Close video", Modifier.size(16.dp))
            }
        }
    }
}

/** Round button that turns white when the remote focus is on it. */
@Composable
private fun ControlButton(
    onClick: () -> Unit,
    size: Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(size),
        colors = IconButtonDefaults.colors(
            containerColor = Color.White.copy(alpha = 0.14f),
            contentColor = Color.White,
            focusedContainerColor = Color.White,
            focusedContentColor = Color.Black
        )
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
    }
}

/** The core icon set has no pause icon, so it is drawn here. */
@Composable
private fun PauseIcon() {
    val color = LocalContentColor.current
    Canvas(Modifier.size(14.dp).semantics { contentDescription = "Pause" }) {
        val bar = Size(size.width * 0.28f, size.height)
        drawRect(color, topLeft = Offset(size.width * 0.12f, 0f), size = bar)
        drawRect(color, topLeft = Offset(size.width * 0.60f, 0f), size = bar)
    }
}

/** Seconds to move for one key event: [repeatCount] grows while the key is held down. */
private fun seekStep(repeatCount: Int): Float = when {
    repeatCount == 0 -> 5f
    repeatCount <= 20 -> 2f
    repeatCount <= 50 -> 5f
    else -> 10f
}

private fun formatTime(seconds: Float): String {
    val total = seconds.toInt()
    val time = "%02d:%02d".format(total / 60 % 60, total % 60)
    return if (total >= 3600) "${total / 3600}:$time" else time
}

private fun playerPage(videoId: String, origin: String, startSeconds: Int) = """
<!DOCTYPE html>
<html>
<head>
<meta name="viewport" content="width=device-width, initial-scale=1">
<meta name="referrer" content="strict-origin-when-cross-origin">
<style>html,body{margin:0;padding:0;border:0;width:100%;height:100%;background:#000;overflow:hidden}#player{position:absolute;top:-65px;left:0;width:100%;height:calc(100% + 130px);border:0;pointer-events:none}</style>
</head>
<body>
<iframe id="player"
  src="https://www.youtube.com/embed/$videoId?enablejsapi=1&autoplay=1&playsinline=1&controls=0&disablekb=1&fs=0&rel=0&iv_load_policy=3&start=$startSeconds&origin=$origin"
  allow="autoplay; encrypted-media"
  referrerpolicy="strict-origin-when-cross-origin"></iframe>
<script src="https://www.youtube.com/iframe_api"></script>
<script>
var player;
function onYouTubeIframeAPIReady() {
  player = new YT.Player('player', {
    events: {
      // YouTube can start muted, and its own mute button is hidden, so turn the sound on here.
      onReady: function (e) { e.target.unMute(); e.target.setVolume(100); e.target.playVideo(); }
    }
  });
}
// The player object exists before its methods do.
function ready() { return player && player.getPlayerState; }
function play() { if (ready()) player.playVideo(); }
function pause() { if (ready()) player.pauseVideo(); }
function toggle() {
  if (ready()) { if (player.getPlayerState() === 1) pause(); else play(); }
}
function seekTo(seconds) {
  if (ready()) player.seekTo(seconds, true);
}
function playback() {
  if (!ready()) return null;
  return [player.getPlayerState(), player.getCurrentTime() || 0, player.getDuration() || 0];
}
</script>
</body>
</html>
"""
