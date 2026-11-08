// TvMediaBridge.kt
// =============================================================================
// Hybrid media bridge - School's simplicity + School2's enhancements.
// 
// This class provides direct JavaScript evaluation for media control,
// combining the reliability of School's approach with School2's
// enhanced features (YouTube nocookie support, better fullscreen).
// =============================================================================

package com.ahead.school.bridge

import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.annotation.Keep


/**
 * State of media playback.
 * 
 * @param isPlaying Whether media is currently playing
 * @param currentTime Current playback position in seconds
 * @param duration Total duration in seconds
 * @param hasMedia Whether media is detected on the page
 * @param isFullscreen Whether media is in fullscreen mode
 * @param source URL of the media source
 */
@Keep
data class TvMediaState(
    val isPlaying: Boolean = false,
    val currentTime: Float = 0f,
    val duration: Float = 0f,
    val hasMedia: Boolean = false,
    val isFullscreen: Boolean = false,
    val source: String = "none",
)


/**
 * Hybrid media bridge for controlling HTML5 video and YouTube iframes.
 * 
 * This bridge uses direct JavaScript evaluation (School's approach) for
 * reliability, while supporting enhanced features from School2:
 * - YouTube nocookie domains
 * - Better fullscreen handling
 * - CSS-based fullscreen fallback
 * - Comprehensive state tracking
 * 
 * @param onMediaStateUpdate Callback for media state changes
 */
@Keep
class TvMediaBridge(private val onMediaStateUpdate: (TvMediaState) -> Unit) {

    // Current media state
    private var currentState = TvMediaState()

    /**
     * Called from JavaScript when media state changes.
     * 
     * @param isPlaying Whether media is playing
     * @param currentTime Current position in seconds
     * @param duration Total duration in seconds
     * @param hasMedia Whether media is present
     * @param isFullscreen Whether in fullscreen
     * @param source URL of the media source
     */
    @Keep
    @Suppress("unused")
    @JavascriptInterface
    fun updateState(
        isPlaying: Boolean,
        currentTime: Double,
        duration: Double,
        hasMedia: Boolean,
        isFullscreen: Boolean,
        source: String?,
    ) {
        currentState = TvMediaState(
            isPlaying = isPlaying,
            currentTime = currentTime.toFloat(),
            duration = duration.toFloat(),
            hasMedia = hasMedia,
            isFullscreen = isFullscreen,
            source = source ?: "none",
        )
        onMediaStateUpdate(currentState)
    }

    /**
     * Called from JavaScript when fullscreen state changes.
     * 
     * @param isFullscreen Whether fullscreen is active
     */
    @Keep
    @Suppress("unused")
    @JavascriptInterface
    fun onFullscreenChanged(isFullscreen: Boolean) {
        currentState = currentState.copy(isFullscreen = isFullscreen)
        onMediaStateUpdate(currentState)
    }

    companion object {
        /**
         * Get the JavaScript code for the hybrid media bridge.
         * 
         * This combines School's simple approach with School2's enhancements:
         * - Enhanced YouTube detection (including nocookie domains)
         * - Better fullscreen handling
         * - CSS-based fullscreen fallback
         * - Proper state tracking
         * 
         * @return JavaScript code to inject into WebView
         */
        fun getBridgeScript(): String {
            return """
                (function() {
                    if (window.__tvMediaBridgeInitialized) return;
                    window.__tvMediaBridgeInitialized = true;

                    // State tracking
                    let state = {
                        isPlaying: false,
                        currentTime: 0,
                        duration: 0,
                        hasMedia: false,
                        isFullscreen: false,
                        source: 'none'
                    };
                    let pendingYouTubeSeekTarget = null;
                    let pendingYouTubeSeekUntil = 0;

                    // YouTube domain patterns
                    const YOUTUBE_DOMAINS = ['youtube.com', 'youtube-nocookie.com', 'youtu.be'];

                    // Check if URL is YouTube
                    function isYouTubeUrl(url) {
                        try {
                            const parsed = new URL(url);
                            const host = parsed.hostname.toLowerCase();
                            return YOUTUBE_DOMAINS.some(domain => 
                                host === domain || host.endsWith('.' + domain)
                            );
                        } catch (_) {
                            return false;
                        }
                    }

                    // Find active HTML5 media
                    function getActiveMedia() {
                        const elements = document.querySelectorAll('video, audio');
                        let best = null;
                        let bestScore = -1;

                        for (let i = 0; i < elements.length; i++) {
                            const element = elements[i];
                            const rect = element.getBoundingClientRect();
                            const isAudio = element.tagName.toLowerCase() === 'audio';

                            if (!isAudio && element.paused && !isVisible(element)) continue;

                            const score = (element.paused ? 0 : 1000000) +
                                rect.width * rect.height +
                                (element.readyState > 0 ? 1000 : 0) +
                                (element.currentTime > 0 ? 500 : 0);

                            if (score > bestScore) {
                                best = element;
                                bestScore = score;
                            }
                        }
                        return best;
                    }

                    // Check if element is visible
                    function isVisible(element) {
                        if (!element) return false;
                        const style = window.getComputedStyle(element);
                        if (style.display === 'none' || 
                            style.visibility === 'hidden' || 
                            Number(style.opacity) === 0) {
                            return false;
                        }
                        const rect = element.getBoundingClientRect();
                        return rect.width > 0 && rect.height > 0;
                    }

                    // Get YouTube iframe
                    function getYouTubeIframe() {
                        const frames = document.querySelectorAll('iframe');
                        for (let i = 0; i < frames.length; i++) {
                            if (frames[i].src && isYouTubeUrl(frames[i].src)) {
                                return frames[i];
                            }
                        }
                        return null;
                    }

                    // Prepare YouTube iframe
                    function prepareYouTubeFrame(frame) {
                        if (!frame || !frame.src) return;

                        // Ensure fullscreen attributes
                        if (!frame.allowFullscreen) {
                            frame.allowFullscreen = true;
                        }
                        if (!frame.hasAttribute('webkitallowfullscreen')) {
                            frame.setAttribute('webkitallowfullscreen', 'true');
                        }
                        if (!frame.hasAttribute('mozallowfullscreen')) {
                            frame.setAttribute('mozallowfullscreen', 'true');
                        }

                        // Ensure allow attribute includes fullscreen
                        let allow = (frame.getAttribute('allow') || '').trim();
                        if (!/(^|;\\s*)fullscreen\\b/i.test(allow)) {
                            frame.setAttribute('allow', allow ? allow + '; fullscreen' : 'fullscreen');
                        }

                        // Add YouTube API parameters
                        try {
                            const url = new URL(frame.src);
                            if (!url.searchParams.has('enablejsapi')) {
                                url.searchParams.set('enablejsapi', '1');
                            }
                            if (!url.searchParams.has('fs')) {
                                url.searchParams.set('fs', '0');
                            }
                            if (!url.searchParams.has('origin')) {
                                url.searchParams.set('origin', window.location.origin);
                            }
                            frame.src = url.toString();
                        } catch (_) {}

                        // Set up YouTube message listener
                        setupYouTubeListener(frame);
                    }

                    // Set up YouTube listener
                    function setupYouTubeListener(frame) {
                        if (!frame || !frame.contentWindow) return;
                        frame.contentWindow.postMessage(JSON.stringify({
                            event: 'listening',
                            id: 1,
                            channel: 'school-tv'
                        }), '*');
                    }

                    // Post to YouTube
                    function postToYouTube(frame, message) {
                        if (!frame || !frame.contentWindow) return;
                        try {
                            frame.contentWindow.postMessage(JSON.stringify(message), '*');
                        } catch (_) {}
                    }

                    // Command YouTube
                    function commandYouTube(frame, func, args) {
                        postToYouTube(frame, {
                            event: 'command',
                            func: func,
                            args: args || [],
                            id: 1,
                            channel: 'school-tv'
                        });
                    }

                    // Handle YouTube messages
                    window.addEventListener('message', function(event) {
                        try {
                            const data = JSON.parse(event.data);
                            if (!data || typeof data !== 'object') return;

                            // Find source iframe
                            let frame = null;
                            const frames = document.querySelectorAll('iframe');
                            for (let i = 0; i < frames.length; i++) {
                                if (frames[i].contentWindow === event.source && 
                                    frames[i].src && isYouTubeUrl(frames[i].src)) {
                                    frame = frames[i];
                                    break;
                                }
                            }
                            if (!frame) return;

                            if (data.event === 'infoDelivery' && data.info) {
                                if (data.info.playerState !== undefined) {
                                    state.isPlaying = (data.info.playerState === 1);
                                }
                                if (data.info.currentTime !== undefined) {
                                    if (Date.now() >= pendingYouTubeSeekUntil) {
                                        state.currentTime = data.info.currentTime;
                                        pendingYouTubeSeekTarget = null;
                                    }
                                }
                                if (data.info.duration !== undefined) {
                                    state.duration = data.info.duration;
                                }
                                publishState();
                            }
                        } catch (_) {}
                    });

                    // Publish state to native
                    function publishState() {
                        const media = getActiveMedia();
                        const yt = getYouTubeIframe();
                        const hasMedia = !!media || !!yt;

                        if (hasMedia) {
                            if (media) {
                                state.isPlaying = !media.paused;
                                state.currentTime = media.currentTime || 0;
                                state.duration = media.duration || 0;
                                state.source = media.src || 'html5';
                            } else if (yt) {
                                state.source = yt.src || 'youtube';
                            }
                            state.hasMedia = true;
                        } else {
                            state.hasMedia = false;
                            state.isPlaying = false;
                            state.currentTime = 0;
                            state.duration = 0;
                            state.source = 'none';
                        }

                        if (window.TvBridge && typeof window.TvBridge.updateState === 'function') {
                            window.TvBridge.updateState(
                                state.isPlaying,
                                state.currentTime,
                                state.duration,
                                state.hasMedia,
                                state.isFullscreen,
                                state.source
                            );
                        }
                    }

                    // Fullscreen handling
                    function notifyFullscreenChange() {
                        const isFullscreen = !!(document.fullscreenElement || 
                            document.webkitFullscreenElement || state.cssFullscreenTarget);
                        state.isFullscreen = isFullscreen;
                        
                        if (window.TvBridge && typeof window.TvBridge.onFullscreenChanged === 'function') {
                            window.TvBridge.onFullscreenChanged(isFullscreen);
                        }
                    }

                    // CSS fullscreen target
                    let cssFullscreenTarget = null;
                    let cssFullscreenTargetStyle = null;
                    let cssFullscreenBodyStyle = null;

                    // Enter fullscreen
                    window.__tvEnterFullscreen = function() {
                        if (cssFullscreenTarget) return window.__tvExitFullscreen();

                        const target = getActiveMedia() || getYouTubeIframe();
                        if (!target) return false;

                        try {
                            // Try native fullscreen first
                            if (target.requestFullscreen) {
                                target.requestFullscreen().catch(() => {});
                            } else if (target.webkitRequestFullscreen) {
                                target.webkitRequestFullscreen();
                            }

                            // Fallback to CSS fullscreen for iframes
                            if (target.tagName.toLowerCase() === 'iframe') {
                                cssFullscreenTarget = target;
                                cssFullscreenTargetStyle = target.getAttribute('style');
                                cssFullscreenBodyStyle = document.body ? document.body.getAttribute('style') : null;

                                target.style.setProperty('position', 'fixed', 'important');
                                target.style.setProperty('top', '0', 'important');
                                target.style.setProperty('left', '0', 'important');
                                target.style.setProperty('width', '100vw', 'important');
                                target.style.setProperty('height', '100vh', 'important');
                                target.style.setProperty('z-index', '2147483647', 'important');
                                target.style.setProperty('background', 'black', 'important');

                                if (document.body) {
                                    document.body.style.setProperty('overflow', 'hidden', 'important');
                                }
                            }

                            notifyFullscreenChange();
                            return true;
                        } catch (_) {
                            cssFullscreenTarget = null;
                            return false;
                        }
                    };

                    // Exit fullscreen
                    window.__tvExitFullscreen = function() {
                        try {
                            if (cssFullscreenTarget) {
                                if (cssFullscreenTargetStyle === null) {
                                    cssFullscreenTarget.removeAttribute('style');
                                } else {
                                    cssFullscreenTarget.setAttribute('style', cssFullscreenTargetStyle);
                                }
                                if (document.body) {
                                    if (cssFullscreenBodyStyle === null) {
                                        document.body.removeAttribute('style');
                                    } else {
                                        document.body.setAttribute('style', cssFullscreenBodyStyle);
                                    }
                                }
                                cssFullscreenTarget = null;
                                cssFullscreenTargetStyle = null;
                                cssFullscreenBodyStyle = null;
                            }

                            if (document.exitFullscreen) {
                                document.exitFullscreen();
                            } else if (document.webkitExitFullscreen) {
                                document.webkitExitFullscreen();
                            }

                            notifyFullscreenChange();
                            return true;
                        } catch (_) {
                            return false;
                        }
                    };

                    // Media control functions
                    window.__tvPlayPause = function() {
                        const media = getActiveMedia();
                        if (media) {
                            if (media.paused) {
                                const promise = media.play();
                                if (promise && typeof promise.catch === 'function') {
                                    promise.catch(() => {});
                                }
                            } else {
                                media.pause();
                            }
                            publishState();
                            return;
                        }

                        const yt = getYouTubeIframe();
                        if (yt) {
                            commandYouTube(yt, state.isPlaying ? 'pauseVideo' : 'playVideo');
                            state.isPlaying = !state.isPlaying;
                            publishState();
                        }
                    };

                    window.__tvSeekBy = function(delta) {
                        const amount = Number(delta);
                        if (!isFinite(amount)) return;

                        const media = getActiveMedia();
                        if (media) {
                            const currentTime = Number.isFinite(media.currentTime) ? media.currentTime : 0;
                            const duration = Number.isFinite(media.duration) && media.duration > 0
                                ? media.duration
                                : null;
                            const requestedTime = Math.max(0, currentTime + amount);
                            const newTime = duration === null
                                ? requestedTime
                                : Math.min(duration, requestedTime);
                            media.currentTime = newTime;
                            publishState();
                            return;
                        }

                        const yt = getYouTubeIframe();
                        if (yt) {
                            const now = Date.now();
                            const currentTime = now < pendingYouTubeSeekUntil &&
                                pendingYouTubeSeekTarget !== null
                                ? pendingYouTubeSeekTarget
                                : state.currentTime;
                            const requestedTime = Math.max(0, currentTime + amount);
                            const targetTime = state.duration > 0
                                ? Math.min(state.duration, requestedTime)
                                : requestedTime;

                            // Keep rapid button presses relative to the latest requested seek,
                            // even before the YouTube iframe reports its updated playback time.
                            pendingYouTubeSeekTarget = targetTime;
                            pendingYouTubeSeekUntil = now + 1500;
                            state.currentTime = targetTime;
                            commandYouTube(yt, 'seekTo', [targetTime, true]);
                            publishState();
                        }
                    };

                    window.__tvSetSpeed = function(rate) {
                        const value = Number(rate);
                        if (!isFinite(value) || value <= 0) return;

                        const media = getActiveMedia();
                        if (media) {
                            media.playbackRate = value;
                            publishState();
                            return;
                        }

                        const yt = getYouTubeIframe();
                        if (yt) {
                            commandYouTube(yt, 'setPlaybackRate', [value]);
                        }
                    };

                    window.__tvSetQuality = function(quality) {
                        const yt = getYouTubeIframe();
                        if (yt) {
                            commandYouTube(yt, 'setPlaybackQuality', [String(quality || 'default')]);
                        }
                    };

                    // Set up event listeners
                    document.addEventListener('play', publishState, true);
                    document.addEventListener('playing', publishState, true);
                    document.addEventListener('pause', publishState, true);
                    document.addEventListener('ended', publishState, true);
                    document.addEventListener('ratechange', publishState, true);
                    document.addEventListener('loadedmetadata', publishState, true);
                    document.addEventListener('durationchange', publishState, true);
                    document.addEventListener('fullscreenchange', notifyFullscreenChange, true);
                    document.addEventListener('webkitfullscreenchange', notifyFullscreenChange, true);

                    // Set up MutationObserver for dynamic content
                    if (window.MutationObserver) {
                        new MutationObserver(function(mutations) {
                            mutations.forEach(function(mutation) {
                                if (mutation.type === 'childList') {
                                    mutation.addedNodes.forEach(function(node) {
                                        if (node.nodeName === 'IFRAME') {
                                            prepareYouTubeFrame(node);
                                        }
                                        if (node.querySelectorAll) {
                                            const iframes = node.querySelectorAll('iframe');
                                            iframes.forEach(prepareYouTubeFrame);
                                        }
                                    });
                                }
                            });
                        }).observe(document.body, { childList: true, subtree: true });
                    }

                    // Prepare existing YouTube iframes
                    const existingFrames = document.querySelectorAll('iframe');
                    existingFrames.forEach(prepareYouTubeFrame);

                    // Initial state publish
                    publishState();

                    // Set up periodic polling
                    setInterval(publishState, 500);
                })();
            """.trimIndent()
        }
    }
}


/**
 * Extension function for WebView to toggle play/pause.
 */
fun WebView.tvPlayPause() = evaluateJavascript("window.__tvPlayPause && window.__tvPlayPause();", null)

/**
 * Extension function for WebView to seek by delta seconds.
 */
fun WebView.tvSeekBy(delta: Double) = evaluateJavascript("window.__tvSeekBy && window.__tvSeekBy($delta);", null)

/**
 * Extension function for WebView to set playback speed.
 */
fun WebView.tvSetSpeed(rate: Double) = evaluateJavascript("window.__tvSetSpeed && window.__tvSetSpeed($rate);", null)

/**
 * Extension function for WebView to set video quality.
 */
fun WebView.tvSetQuality(quality: String) {
    val escaped = org.json.JSONObject.quote(quality)
    evaluateJavascript(
        "window.__tvSetQuality && window.__tvSetQuality($escaped);",
        null
    )
}

/**
 * Extension function for WebView to enter fullscreen.
 */
fun WebView.tvEnterFullscreen() = evaluateJavascript("window.__tvEnterFullscreen && window.__tvEnterFullscreen();", null)

/**
 * Extension function for WebView to exit fullscreen.
 */
fun WebView.tvExitFullscreen() = evaluateJavascript("window.__tvExitFullscreen && window.__tvExitFullscreen();", null)
