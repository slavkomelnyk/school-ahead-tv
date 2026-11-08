// TvWebViewScreen.kt
// =============================================================================
// Hybrid WebView screen - School's simplicity + School2's features.
// 
// This composable provides a WebView with:
// - School's clean architecture and reliable media detection
// - School2's always-visible overlay (when not fullscreen)
// - School2's no-tinting design
// - School2's back button support
// - School2's enhanced YouTube support
// =============================================================================

package com.ahead.school.ui.screens.web

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.SystemClock
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.ahead.school.R
import com.ahead.school.MainActivity
import com.ahead.school.bridge.TvMediaBridge
import com.ahead.school.bridge.TvMediaState
import com.ahead.school.bridge.tvEnterFullscreen
import com.ahead.school.bridge.tvExitFullscreen
import com.ahead.school.bridge.tvPlayPause
import com.ahead.school.bridge.tvSeekBy
import com.ahead.school.bridge.tvSetQuality
import com.ahead.school.bridge.tvSetSpeed
import com.ahead.school.core.findActivity
import com.ahead.school.model.MediaState
import com.ahead.school.ui.screens.media.MediaOverlay
import java.net.URLEncoder

private const val BACK_DOUBLE_PRESS_WINDOW_MS = 2_000L


/**
 * Converts TvMediaState to MediaState for use with MediaOverlay.
 */
private fun TvMediaState.toMediaState(): MediaState {
    return MediaState(
        hasMedia = this.hasMedia,
        isPlaying = this.isPlaying,
        currentTime = this.currentTime,
        duration = this.duration,
        playbackRate = 1f, // TvMediaState doesn't track playback rate
        source = this.source,
        isFullscreen = this.isFullscreen,
    )
}


/**
 * Hybrid WebView screen with enhanced media controls.
 * 
 * Features:
 * - School's reliable media detection and control
 * - School2's smart overlay visibility with auto-show state
 * - School2's no-tinting transparent overlay
 * - School2's back button support
 * - School2's enhanced YouTube support (nocookie domains)
 * - Close button to hide overlay
 * - MENU button to toggle overlay
 * 
 * @param url The URL to load in the WebView
 * @param modifier Modifier for the composable
 */
@SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
@Composable
fun TvWebViewScreen(
    url: String,
    modifier: Modifier = Modifier,
) {
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mainActivity = remember(context) { context.findActivity() as? MainActivity }

    // Media state from bridge
    var tvMediaState by remember { mutableStateOf(TvMediaState()) }
    
    // Overlay visibility logic:
    // - Track whether user wants auto-show (default: true)
    // - Reset auto-show flag when page or video changes
    var isOverlayVisible by remember { mutableStateOf(false) }
    var autoShowOverlay by remember { mutableStateOf(true) }
    var lastMediaSource by remember { mutableStateOf<String?>(null) }
    var lastBackPressedAt by remember { mutableStateOf(0L) }

    // Media bridge
    val mediaBridge = remember {
        TvMediaBridge { state ->
            val sourceChanged = state.source != lastMediaSource
            lastMediaSource = state.source
            
            tvMediaState = state
            
            // Auto-show overlay when media is detected, not in fullscreen, and auto-show is enabled
            if (state.hasMedia && !state.isFullscreen && autoShowOverlay) {
                isOverlayVisible = true
            } else if (!state.hasMedia) {
                // Hide overlay when no media
                isOverlayVisible = false
                // Reset auto-show when media is removed (page change)
                autoShowOverlay = true
            }
            
            // Reset auto-show flag when video source changes
            if (sourceChanged && state.hasMedia) {
                autoShowOverlay = true
            }
        }
    }

    fun hideMediaControls() {
        isOverlayVisible = false
        autoShowOverlay = false
        webViewInstance?.requestFocus()
    }

    DisposableEffect(mainActivity) {
        onDispose { mainActivity?.setAppKeyEventHandler(null) }
    }

    SideEffect {
        mainActivity?.setAppKeyEventHandler { event ->
            if (event.action != KeyEvent.ACTION_DOWN || event.repeatCount > 0 || !tvMediaState.hasMedia) {
                false
            } else {
                when (event.keyCode) {
                    KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                    KeyEvent.KEYCODE_MEDIA_PLAY,
                    KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                        webViewInstance?.tvPlayPause()
                        webViewInstance != null
                    }
                    KeyEvent.KEYCODE_MEDIA_REWIND -> {
                        webViewInstance?.tvSeekBy(-10.0)
                        webViewInstance != null
                    }
                    KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                        webViewInstance?.tvSeekBy(10.0)
                        webViewInstance != null
                    }
                    KeyEvent.KEYCODE_DPAD_LEFT,
                    KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        if (isOverlayVisible) {
                            false
                        } else {
                            webViewInstance?.tvSeekBy(
                                if (event.keyCode == KeyEvent.KEYCODE_DPAD_LEFT) -10.0 else 10.0
                            )
                            webViewInstance != null
                        }
                    }
                    KeyEvent.KEYCODE_MENU -> {
                        if (isOverlayVisible) {
                            hideMediaControls()
                        } else {
                            isOverlayVisible = true
                            autoShowOverlay = false
                        }
                        true
                    }
                    KeyEvent.KEYCODE_DPAD_CENTER,
                    KeyEvent.KEYCODE_ENTER -> {
                        if (tvMediaState.isFullscreen && !isOverlayVisible) {
                            isOverlayVisible = true
                            autoShowOverlay = false
                            true
                        } else {
                            false
                        }
                    }
                    else -> false
                }
            }
        }
    }


    // ---------------------------------------------------------------------------
    // Lifecycle Management
    // ---------------------------------------------------------------------------

    DisposableEffect(lifecycleOwner, webViewInstance) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                webViewInstance?.let { webView ->
                    webView.requestFocus()
                    webView.evaluateJavascript(
                        "if (window.ensureFocus) { window.ensureFocus(); }",
                        null
                    )
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }


    // ---------------------------------------------------------------------------
    // Network Connectivity
    // ---------------------------------------------------------------------------

    DisposableEffect(context) {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                context.findActivity()?.runOnUiThread {
                    if (hasError) {
                        hasError = false
                        webViewInstance?.reload()
                    }
                }
            }
        }

        connectivityManager.registerNetworkCallback(request, networkCallback)
        onDispose {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        }
    }


    // ---------------------------------------------------------------------------
    // Back Button Handling
    // ---------------------------------------------------------------------------

    BackHandler(enabled = true) {
        val now = SystemClock.elapsedRealtime()
        when {
            isOverlayVisible -> {
                hideMediaControls()
                lastBackPressedAt = now
            }
            lastBackPressedAt != 0L && now - lastBackPressedAt <= BACK_DOUBLE_PRESS_WINDOW_MS -> {
                lastBackPressedAt = 0L
                context.findActivity()?.moveTaskToBack(true)
            }
            else -> {
                lastBackPressedAt = now
                when {
                    tvMediaState.isFullscreen -> webViewInstance?.tvExitFullscreen()
                    webViewInstance?.canGoBack() == true -> webViewInstance?.goBack()
                    else -> Toast.makeText(
                        context,
                        R.string.back_again_to_minimize,
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
        }
    }


    // ---------------------------------------------------------------------------
    // Main UI
    // ---------------------------------------------------------------------------

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        // WebView
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )

                    isFocusable = true
                    isFocusableInTouchMode = true
                    setLayerType(View.LAYER_TYPE_HARDWARE, null)

                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        cacheMode = WebSettings.LOAD_DEFAULT
                        mediaPlaybackRequiresUserGesture = false
                        userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                    }

                    val cookieManager = CookieManager.getInstance()
                    cookieManager.setAcceptCookie(true)
                    cookieManager.setAcceptThirdPartyCookies(this, true)

                    addJavascriptInterface(mediaBridge, "TvBridge")

                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            isLoading = true
                            hasError = false
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?
                        ) {
                            super.onReceivedError(view, request, error)
                            if (request?.isForMainFrame == true) {
                                isLoading = false
                                hasError = true
                                errorMessage = error?.description?.toString() ?: "Network error occurred."
                            }
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            if (!hasError) {
                                isLoading = false
                                injectStylesAndScripts(view)
                            }
                        }

                        private fun injectStylesAndScripts(view: WebView?) {
                            view?.let { webView ->
                                // Inject CSS
                                val css = try {
                                    context.assets.open("inject/tv_styles.css").bufferedReader().use { it.readText() }
                                } catch (_: Exception) {
                                    ""
                                }

                                // Inject focus observer
                                val focusJs = try {
                                    context.assets.open("inject/focus_observer.js").bufferedReader().use { it.readText() }
                                } catch (_: Exception) {
                                    ""
                                }

                                // Inject media bridge
                                val bridgeJs = TvMediaBridge.getBridgeScript()

                                if (css.isNotEmpty()) {
                                    val encodedCss = URLEncoder.encode(css, "UTF-8")
                                    webView.loadUrl(
                                        "javascript:(function() {" +
                                        "var parent = document.getElementsByTagName('head')[0];" +
                                        "var style = document.createElement('style');" +
                                        "style.type = 'text/css';" +
                                        "style.innerHTML = decodeURIComponent('$encodedCss');" +
                                        "parent.appendChild(style);" +
                                        "})();"
                                    )
                                }

                                if (focusJs.isNotEmpty()) {
                                    webView.loadUrl("javascript:$focusJs")
                                }

                                if (bridgeJs.isNotEmpty()) {
                                    webView.evaluateJavascript(bridgeJs, null)
                                }

                                webView.requestFocus()
                            }
                        }
                    }

                    webChromeClient = object : WebChromeClient() {
                        private var customView: View? = null
                        private var customViewCallback: CustomViewCallback? = null

                        override fun onShowCustomView(
                            view: View?,
                            callback: CustomViewCallback?
                        ) {
                            if (customView != null) {
                                callback?.onCustomViewHidden()
                                return
                            }
                            customView = view
                            customViewCallback = callback
                            
                            context.findActivity()?.addContentView(
                                customView,
                                ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            )
                        }

                        override fun onHideCustomView() {
                            (customView?.parent as? ViewGroup)?.removeView(customView)
                            customView = null
                            customViewCallback?.onCustomViewHidden()
                        }
                    }

                    loadUrl(url)
                    webViewInstance = this
                }
            },
            update = { view ->
                webViewInstance = view
            }
        )

        // Loading indicator
        if (isLoading && !hasError) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                // Loading indicator would go here
                // For now, just show black screen
            }
        }

        // Error screen
        if (hasError) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                // ErrorScreen would go here
            }
        }

        // Media overlay - visible when media is present
        MediaOverlay(
            state = tvMediaState.toMediaState(),
            visible = isOverlayVisible,
            onPlayPause = { webViewInstance?.tvPlayPause() },
            onSeekBackward = { webViewInstance?.tvSeekBy(-10.0) },
            onSeekForward = { webViewInstance?.tvSeekBy(10.0) },
            onFullscreen = { 
                if (tvMediaState.isFullscreen) {
                    webViewInstance?.tvExitFullscreen()
                } else {
                    webViewInstance?.tvEnterFullscreen()
                }
            },
            onExitFullscreen = { webViewInstance?.tvExitFullscreen() },
            onCloseOverlay = { hideMediaControls() },
        )
    }
}
