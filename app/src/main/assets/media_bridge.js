// =============================================================================
// Media Bridge for Android TV
// =============================================================================
// This script is injected into web pages to bridge native TV controls to
// HTML5 media elements and YouTube iframe players.
//
// Features:
// - Detects and controls HTML5 <video> and <audio> elements
// - Detects and controls YouTube iframe embeds
// - Provides fullscreen management
// - Handles dynamically added iframes (SPA support)
// - Works with various YouTube URL patterns
// =============================================================================

(function () {
  'use strict';

  // ---------------------------------------------------------------------------
  // Guard: Prevent double injection
  // ---------------------------------------------------------------------------
  if (window.__tvBridgeInstalled) return;
  window.__tvBridgeInstalled = true;


  // =============================================================================
  // Configuration
  // =============================================================================

  var POLL_INTERVAL_MS = 500;            // Polling interval for HTML5 media
  var YOUTUBE_POLL_INTERVAL_MS = 1000;   // Polling interval for YouTube iframes
  var YOUTUBE_API_TIMEOUT_MS = 150;      // Delay before setting up YouTube API


  // =============================================================================
  // State Management
  // =============================================================================

  var state = {
    // Media state tracking
    lastSignature: '',
    lastYouTubePoll: 0,
    trackedYouTube: null,

    // Fullscreen state
    cssFullscreenTarget: null,
    cssFullscreenTargetStyle: null,
    cssFullscreenBodyStyle: null,
    nativeFullscreen: false,

    // YouTube player states (WeakMap to avoid memory leaks)
    youtubeStates: new WeakMap(),

    // Mutation observer for dynamic content
    observer: null
  };


  // =============================================================================
  // Utility Functions
  // =============================================================================

  /**
   * Check if an element is visible in the DOM
   * @param {HTMLElement} element - The element to check
   * @returns {boolean} - True if visible, false otherwise
   */
  function isVisible(element) {
    if (!element) return false;

    var style = window.getComputedStyle(element);
    if (style.display === 'none' ||
        style.visibility === 'hidden' ||
        Number(style.opacity) === 0) {
      return false;
    }

    var rect = element.getBoundingClientRect();
    return rect.width > 0 && rect.height > 0;
  }

  /**
   * Extract the source URL from a media element
   * @param {HTMLMediaElement} element - The media element
   * @returns {string} - The source URL
   */
  function getMediaSource(element) {
    if (!element) return 'none';

    var src = element.currentSrc || element.src || '';
    if (!src) {
      var source = element.querySelector('source');
      if (source) src = source.src || '';
    }
    return src || 'html5';
  }


  // =============================================================================
  // YouTube URL Detection
  // =============================================================================

  /**
   * YouTube domain patterns for detection
   */
  var YOUTUBE_DOMAINS = [
    'youtube.com',
    'youtube-nocookie.com',
    'youtu.be'
  ];

  /**
   * YouTube embed path patterns for detection
   */
  var YOUTUBE_EMBED_PATTERNS = [
    '/embed/',
    '/v/',
    '/watch',
    '/live/',
    '/shorts/'
  ];

  /**
   * Check if a URL is a YouTube embed URL
   * @param {string} url - The URL to check
   * @returns {boolean} - True if it's a YouTube embed URL
   */
  function isYouTubeUrl(url) {
    try {
      var parsed = new URL(url);
      var host = parsed.hostname.toLowerCase();
      var pathname = parsed.pathname.toLowerCase();

      // Check if it's a YouTube domain
      var isYouTubeDomain = YOUTUBE_DOMAINS.some(function(domain) {
        return host === domain || host.endsWith('.' + domain);
      });

      if (!isYouTubeDomain) return false;

      // Check for embed path patterns
      for (var i = 0; i < YOUTUBE_EMBED_PATTERNS.length; i++) {
        if (pathname.indexOf(YOUTUBE_EMBED_PATTERNS[i]) !== -1) {
          return true;
        }
      }

      // For youtu.be, check if pathname looks like a video ID (11 chars)
      if (host.indexOf('youtu.be') !== -1 && pathname.match(/^\/[a-zA-Z0-9_-]{11}$/)) {
        return true;
      }

      return false;
    } catch (_) {
      return false;
    }
  }

  /**
   * Check if a frame is a YouTube iframe
   * @param {HTMLIFrameElement} frame - The iframe element
   * @returns {boolean} - True if it's a YouTube iframe
   */
  function isYouTubeFrame(frame) {
    if (!frame || !frame.src) return false;
    return isYouTubeUrl(frame.src);
  }


  // =============================================================================
  // YouTube State Management
  // =============================================================================

  /**
   * Create initial state for a YouTube player
   * @returns {Object} - Initial state object
   */
  function createYouTubeState() {
    return {
      isPlaying: false,
      currentTime: 0,
      duration: 0,
      playbackRate: 1
    };
  }

  /**
   * Get or create state for a YouTube frame
   * @param {HTMLIFrameElement} frame - The YouTube iframe
   * @returns {Object} - The state object
   */
  function getYouTubeState(frame) {
    var playerState = state.youtubeStates.get(frame);
    if (!playerState) {
      playerState = createYouTubeState();
      state.youtubeStates.set(frame, playerState);
    }
    return playerState;
  }


  // =============================================================================
  // YouTube Iframe Preparation
  // =============================================================================

  /**
   * Add a parameter to a URL if not already present
   * @param {URL} url - The URL object
   * @param {string} param - The parameter name
   * @param {string} value - The parameter value
   * @returns {boolean} - True if the parameter was added or changed
   */
  function ensureUrlParam(url, param, value) {
    var current = url.searchParams.get(param);
    if (current !== value) {
      url.searchParams.set(param, value);
      return true;
    }
    return false;
  }

  /**
   * Prepare a YouTube iframe with proper parameters and attributes
   * @param {HTMLIFrameElement} frame - The YouTube iframe
   */
  function prepareYouTubeFrame(frame) {
    if (!isYouTubeFrame(frame)) return;

    var needsReload = false;

    // Set fullscreen attributes on the iframe element
    if (!frame.allowFullscreen) {
      frame.allowFullscreen = true;
      needsReload = true;
    }
    if (!frame.hasAttribute('webkitallowfullscreen')) {
      frame.setAttribute('webkitallowfullscreen', 'true');
      needsReload = true;
    }
    if (!frame.hasAttribute('mozallowfullscreen')) {
      frame.setAttribute('mozallowfullscreen', 'true');
      needsReload = true;
    }

    var sandbox = frame.getAttribute('sandbox');
    if (sandbox) {
      var tokens = sandbox.split(/\s+/);
      if (tokens.indexOf('allow-fullscreen') === -1) {
        frame.setAttribute('sandbox', sandbox + ' allow-fullscreen');
        needsReload = true;
      }
    }

    var allow = (frame.getAttribute('allow') || '').trim();
    if (!/(^|;\s*)fullscreen\b/i.test(allow)) {
      frame.setAttribute('allow', allow ? allow + '; fullscreen' : 'fullscreen');
      needsReload = true;
    }

    var src;
    try {
      src = new URL(frame.src, document.baseURI);
    } catch (_) {
      return;
    }

    var changed = false;
    changed = ensureUrlParam(src, 'enablejsapi', '1') || changed;

    if (!src.searchParams.has('origin')) {
      src.searchParams.set('origin', window.location.origin);
      changed = true;
    }

    changed = ensureUrlParam(src, 'fs', '0') || changed;
    changed = ensureUrlParam(src, 'modestbranding', '1') || changed;
    changed = ensureUrlParam(src, 'rel', '0') || changed;
    changed = ensureUrlParam(src, 'iv_load_policy', '3') || changed;

    if ((needsReload || changed) && frame.getAttribute('data-tv-youtube-api') !== 'enabled') {
      frame.setAttribute('data-tv-youtube-api', 'enabled');
      state.youtubeStates.set(frame, createYouTubeState());
      frame.src = src.toString();
      return;
    }

    if (frame.__tvListeningSrc !== frame.src) {
      frame.__tvListeningSrc = frame.src;
      state.youtubeStates.set(frame, createYouTubeState());

      window.setTimeout(function () {
        setupYouTubeListener(frame);
        injectYouTubeStyles(frame);
      }, YOUTUBE_API_TIMEOUT_MS);
    }
  }

  function setupYouTubeListener(frame) {
    postToYouTube(frame, { event: 'listening', id: 1, channel: 'school-tv' });
  }

  function postToYouTube(frame, message) {
    if (!frame || !frame.contentWindow) return;
    try {
      frame.contentWindow.postMessage(JSON.stringify(message), '*');
    } catch (_) {}
  }

  function commandYouTube(frame, func, args) {
    postToYouTube(frame, {
      event: 'command',
      func: func,
      args: args || [],
      id: 1,
      channel: 'school-tv'
    });
  }

  function isYouTubeOrigin(origin) {
    try {
      var host = new URL(origin).hostname.toLowerCase();
      return YOUTUBE_DOMAINS.some(function(domain) {
        return host === domain || host.endsWith('.' + domain);
      });
    } catch (_) {
      return false;
    }
  }


  // =============================================================================
  // YouTube Style Injection
  // =============================================================================

  function injectYouTubeStyles(frame) {
    try {
      if (!frame.contentDocument || !frame.contentWindow) return;
      var doc = frame.contentDocument;
      if (!doc) return;

      if (doc.getElementById('__tvYouTubeStyles')) return;

      var style = doc.createElement('style');
      style.id = '__tvYouTubeStyles';
      style.textContent = [
        '.ytp-fullscreen-button,',
        'button[aria-label*="fullscreen"],',
        'button[aria-label*="Fullscreen"] {',
        '  display: none !important;',
        '  visibility: hidden !important;',
        '}',
        '.ytp-button.fullscreen,',
        '.ytp-fullscreen {',
        '  display: none !important;',
        '}',
        '#player-container,',
        '#player-api-frame,',
        '.html5-video-container {',
        '  pointer-events: auto !important;',
        '}'
      ].join('\n');

      if (doc.head) {
        doc.head.appendChild(style);
      } else if (doc.documentElement) {
        doc.documentElement.appendChild(style);
      }
    } catch (_) {}
  }


  // =============================================================================
  // Media Discovery
  // =============================================================================

  function findHtmlMedia() {
    var elements = document.querySelectorAll('video, audio');
    var best = null;
    var bestScore = -1;

    for (var i = 0; i < elements.length; i++) {
      var element = elements[i];
      var rect = element.getBoundingClientRect();
      var isAudio = element.tagName.toLowerCase() === 'audio';

      if (!isAudio && element.paused && !isVisible(element)) continue;

      var score = (element.paused ? 0 : 1000000) +
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

  function findYouTubeFrames() {
    var frames = document.querySelectorAll('iframe');
    var candidates = [];
    var best = null;
    var bestArea = -1;

    for (var i = 0; i < frames.length; i++) {
      var frame = frames[i];
      if (!isYouTubeFrame(frame)) continue;

      prepareYouTubeFrame(frame);
      if (!isVisible(frame)) continue;

      candidates.push(frame);
      var rect = frame.getBoundingClientRect();
      var area = rect.width * rect.height;

      if (area > bestArea) {
        best = frame;
        bestArea = area;
      }
    }
    return { all: candidates, active: best };
  }


  // =============================================================================
  // Message Handling
  // =============================================================================

  function handleYouTubeMessage(event) {
    var frame = null;
    var frames = document.querySelectorAll('iframe');

    for (var i = 0; i < frames.length; i++) {
      if (frames[i].contentWindow === event.source && isYouTubeFrame(frames[i])) {
        frame = frames[i];
        break;
      }
    }

    if (!frame || !isYouTubeOrigin(event.origin)) return;

    var data = event.data;
    try {
      if (typeof data === 'string') data = JSON.parse(data);
    } catch (_) { return; }
    if (!data || typeof data !== 'object') return;

    if (data.event === 'onReady') {
      setupYouTubeListener(frame);
    }

    if (data.event !== 'infoDelivery' || !data.info) return;

    var playerState = getYouTubeState(frame);
    if (typeof data.info.playerState === 'number') {
      playerState.isPlaying = data.info.playerState === 1;
    }
    if (typeof data.info.currentTime === 'number' && isFinite(data.info.currentTime)) {
      playerState.currentTime = Math.max(0, data.info.currentTime);
    }
    if (typeof data.info.duration === 'number' && isFinite(data.info.duration)) {
      playerState.duration = Math.max(0, data.info.duration);
    }
    if (typeof data.info.playbackRate === 'number' && isFinite(data.info.playbackRate)) {
      playerState.playbackRate = data.info.playbackRate;
    }
  }

  window.addEventListener('message', handleYouTubeMessage);


  // =============================================================================
  // State Publishing & Fullscreen Notifications
  // =============================================================================

  function publishState() {
    var htmlMedia = findHtmlMedia();
    var ytResult = findYouTubeFrames();
    var ytFrame = ytResult.active;

    var hasMedia = false;
    var isPlaying = false;
    var currentTime = 0;
    var duration = 0;
    var playbackRate = 1;
    var source = 'none';

    if (htmlMedia) {
      hasMedia = true;
      isPlaying = !htmlMedia.paused;
      currentTime = htmlMedia.currentTime || 0;
      duration = htmlMedia.duration || 0;
      playbackRate = htmlMedia.playbackRate || 1;
      source = getMediaSource(htmlMedia);
    } else if (ytFrame) {
      var ytState = getYouTubeState(ytFrame);
      hasMedia = true;
      isPlaying = ytState.isPlaying;
      currentTime = ytState.currentTime;
      duration = ytState.duration;
      playbackRate = ytState.playbackRate;
      source = ytFrame.src;
    }

    var signature = [
      hasMedia ? 1 : 0,
      isPlaying ? 1 : 0,
      Math.round(currentTime * 4),
      Math.round(duration),
      playbackRate,
      source
    ].join('|');

    if (signature === state.lastSignature) return;
    state.lastSignature = signature;

    try {
      if (window.MediaBridge && typeof window.MediaBridge.onStateChanged === 'function') {
        window.MediaBridge.onStateChanged(
            hasMedia,
            isPlaying,
            currentTime,
            duration,
            playbackRate,
            source
        );
      }
    } catch (_) {}
  }

  function notifyFullscreenChange() {
    var isFullscreen = !!(state.cssFullscreenTarget || state.nativeFullscreen ||
        document.fullscreenElement || document.webkitFullscreenElement);

    try {
      if (window.MediaBridge && typeof window.MediaBridge.onFullscreenChanged === 'function') {
        window.MediaBridge.onFullscreenChanged(isFullscreen);
      }
    } catch (_) {}
  }


  // =============================================================================
  // Public Control API
  // =============================================================================

  window.__tvPlayPause = function() {
    var media = findHtmlMedia();
    if (media) {
      if (media.paused) {
        var p = media.play();
        if (p && typeof p.catch === 'function') p.catch(function(){});
      } else {
        media.pause();
      }
      publishState();
      return;
    }

    var ytResult = findYouTubeFrames();
    if (ytResult.active) {
      var ytState = getYouTubeState(ytResult.active);
      commandYouTube(ytResult.active, ytState.isPlaying ? 'pauseVideo' : 'playVideo');
      ytState.isPlaying = !ytState.isPlaying;
      publishState();
    }
  };

  window.__tvSeekBy = function(delta) {
    var amount = Number(delta);
    if (!isFinite(amount)) return;

    var media = findHtmlMedia();
    if (media) {
      media.currentTime = Math.max(0, Math.min(media.duration || 0, media.currentTime + amount));
      publishState();
      return;
    }

    var ytResult = findYouTubeFrames();
    if (ytResult.active) {
      var ytState = getYouTubeState(ytResult.active);
      var targetTime = Math.max(0, ytState.currentTime + amount);
      commandYouTube(ytResult.active, 'seekTo', [targetTime, true]);
    }
  };

  window.__tvSetSpeed = function(rate) {
    var value = Number(rate);
    if (!isFinite(value) || value <= 0) return;

    var media = findHtmlMedia();
    if (media) {
      media.playbackRate = value;
      publishState();
      return;
    }

    var ytResult = findYouTubeFrames();
    if (ytResult.active) {
      commandYouTube(ytResult.active, 'setPlaybackRate', [value]);
    }
  };

  window.__tvEnterFullscreen = function() {
    if (state.cssFullscreenTarget) return window.__tvExitFullscreen();

    var target = findHtmlMedia() || findYouTubeFrames().active;
    if (!target) return false;

    try {
      if (target.requestFullscreen) {
        target.requestFullscreen().catch(function(){});
      } else if (target.webkitRequestFullscreen) {
        target.webkitRequestFullscreen();
      }

      if (target.tagName.toLowerCase() === 'iframe') {
        state.cssFullscreenTarget = target;
        state.cssFullscreenTargetStyle = target.getAttribute('style');
        state.cssFullscreenBodyStyle = document.body ? document.body.getAttribute('style') : null;

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
      state.cssFullscreenTarget = null;
      return false;
    }
  };

  window.__tvExitFullscreen = function() {
    try {
      if (state.cssFullscreenTarget) {
        if (state.cssFullscreenTargetStyle === null) {
          state.cssFullscreenTarget.removeAttribute('style');
        } else {
          state.cssFullscreenTarget.setAttribute('style', state.cssFullscreenTargetStyle);
        }
        if (document.body) {
          if (state.cssFullscreenBodyStyle === null) {
            document.body.removeAttribute('style');
          } else {
            document.body.setAttribute('style', state.cssFullscreenBodyStyle);
          }
        }
        state.cssFullscreenTarget = null;
        state.cssFullscreenTargetStyle = null;
        state.cssFullscreenBodyStyle = null;
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


  // =============================================================================
  // Event Listeners & Initialization
  // =============================================================================

  document.addEventListener('play', publishState, true);
  document.addEventListener('playing', publishState, true);
  document.addEventListener('pause', publishState, true);
  document.addEventListener('ended', publishState, true);
  document.addEventListener('ratechange', publishState, true);
  document.addEventListener('loadedmetadata', publishState, true);
  document.addEventListener('durationchange', publishState, true);
  document.addEventListener('fullscreenchange', notifyFullscreenChange, true);
  document.addEventListener('webkitfullscreenchange', notifyFullscreenChange, true);

  if (window.MutationObserver) {
    state.observer = new MutationObserver(function(mutations) {
      mutations.forEach(function(mutation) {
        if (mutation.type === 'childList') {
          mutation.addedNodes.forEach(function(node) {
            if (node.nodeName === 'IFRAME') {
              prepareYouTubeFrame(node);
            }
            if (node.querySelectorAll) {
              node.querySelectorAll('iframe').forEach(prepareYouTubeFrame);
            }
          });
        }
      });
    });

    if (document.body) {
      state.observer.observe(document.body, { childList: true, subtree: true });
    }
  }

  // Initial scan and polling setup
  document.querySelectorAll('iframe').forEach(prepareYouTubeFrame);
  setInterval(publishState, POLL_INTERVAL_MS);

  // Poll YouTube status if active
  setInterval(function() {
    var ytResult = findYouTubeFrames();
    if (ytResult.active) {
      setupYouTubeListener(ytResult.active);
    }
  }, YOUTUBE_POLL_INTERVAL_MS);

  publishState();

})();