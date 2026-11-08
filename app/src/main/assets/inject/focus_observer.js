(function () {
    if (window.__tvFocusInstalled) return;
    window.__tvFocusInstalled = true;

    var focusableSelector = [
        'a[href]',
        'area[href]',
        'button:not([disabled])',
        'input:not([disabled]):not([type="hidden"])',
        'select:not([disabled])',
        'textarea:not([disabled])',
        'summary',
        'video[controls]',
        'audio[controls]',
        'iframe',
        '[role="button"]',
        '[role="link"]',
        '[tabindex]:not([tabindex="-1"])',
        '[contenteditable="true"]'
    ].join(',');

    function isVisible(element) {
        if (!element || element.disabled || element.getAttribute('aria-hidden') === 'true') return false;
        var style = window.getComputedStyle(element);
        if (style.display === 'none' || style.visibility === 'hidden' || Number(style.opacity) === 0) {
            return false;
        }
        var rect = element.getBoundingClientRect();
        return rect.width > 0 && rect.height > 0;
    }

    function firstVisibleTarget() {
        var targets = document.querySelectorAll(focusableSelector);
        for (var i = 0; i < targets.length; i++) {
            if (isVisible(targets[i])) return targets[i];
        }
        return null;
    }

    function moveFocusInDirection(source, key) {
        var sourceRect = source.getBoundingClientRect();
        var sourceX = sourceRect.left + sourceRect.width / 2;
        var sourceY = sourceRect.top + sourceRect.height / 2;
        var horizontal = key === 'ArrowLeft' || key === 'ArrowRight';
        var sign = key === 'ArrowLeft' || key === 'ArrowUp' ? -1 : 1;
        var targets = document.querySelectorAll(focusableSelector);
        var best = null;
        var bestScore = Infinity;

        for (var i = 0; i < targets.length; i++) {
            var candidate = targets[i];
            if (candidate === source || source.contains(candidate) || !isVisible(candidate)) continue;

            var rect = candidate.getBoundingClientRect();
            var x = rect.left + rect.width / 2;
            var y = rect.top + rect.height / 2;
            var primary = horizontal ? (x - sourceX) * sign : (y - sourceY) * sign;
            if (primary <= 1) continue;

            var secondary = Math.abs(horizontal ? y - sourceY : x - sourceX);
            var score = primary + secondary * 2.5;
            if (score < bestScore) {
                best = candidate;
                bestScore = score;
            }
        }

        if (!best) return false;
        try {
            best.focus({ preventScroll: true });
            if (document.activeElement !== best) return false;
            best.scrollIntoView({ behavior: 'auto', block: 'nearest', inline: 'nearest' });
            return true;
        } catch (_) {
            return false;
        }
    }

    function onArrowKey(event) {
        if (event.defaultPrevented || event.altKey || event.ctrlKey || event.metaKey) return;
        var key = event.key;
        if (key !== 'ArrowLeft' && key !== 'ArrowRight' && key !== 'ArrowUp' && key !== 'ArrowDown') return;

        var active = document.activeElement;
        if (!active || active === document.body || active === document.documentElement) return;

        var tag = active.tagName ? active.tagName.toLowerCase() : '';
        if (tag === 'textarea' || active.isContentEditable || tag === 'select') return;

        if (tag === 'input' && (key === 'ArrowLeft' || key === 'ArrowRight')) {
            var type = (active.type || 'text').toLowerCase();
            var textTypes = ['text', 'search', 'url', 'tel', 'email', 'password'];
            if (textTypes.indexOf(type) !== -1) {
                try {
                    var start = active.selectionStart;
                    var end = active.selectionEnd;
                    var atEdge = key === 'ArrowLeft'
                        ? start === 0 && start === end
                        : end === active.value.length && start === end;
                    if (!atEdge) return;
                } catch (_) {
                    return;
                }
            }
        }

        if (moveFocusInDirection(active, key)) {
            event.preventDefault();
            event.stopPropagation();
        }
    }

    window.__tvEnsureFocus = function () {
        var active = document.activeElement;
        if (active && active !== document.body && active !== document.documentElement && isVisible(active)) {
            return true;
        }

        var target = firstVisibleTarget();
        if (target) {
            try {
                target.focus({ preventScroll: true });
                return document.activeElement === target;
            } catch (_) {}
        }

        if (document.body) {
            if (!document.body.hasAttribute('tabindex')) document.body.setAttribute('tabindex', '-1');
            try { document.body.focus({ preventScroll: true }); } catch (_) {}
        }
        return false;
    };

    // Keep the old public name for pages that already call it.
    window.ensureFocus = window.__tvEnsureFocus;

    document.addEventListener('focusin', function (e) {
        if (e.target && e.target.scrollIntoView) {
            try { e.target.scrollIntoView({ behavior: 'auto', block: 'nearest', inline: 'nearest' }); } catch (_) {}
        }
    });

    // WebView doesn't consistently move DOM focus with a TV remote's arrows.
    // Run after page handlers so custom player and site navigation can opt out.
    document.addEventListener('keydown', onArrowKey, false);

    // Recover focus after SPA route changes or when a focused control is removed.
    var pending = false;
    var observer = new MutationObserver(function () {
        if (pending) return;
        pending = true;
        requestAnimationFrame(function () {
            pending = false;
            window.__tvEnsureFocus();
        });
    });

    function startObserving() {
        if (!document.body) return;
        observer.observe(document.body, { childList: true, subtree: true });
        window.__tvEnsureFocus();
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', startObserving, { once: true });
    } else {
        startObserving();
    }
})();
