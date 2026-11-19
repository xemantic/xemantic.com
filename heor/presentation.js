(function () {
  'use strict';
  const slides = Array.from(document.querySelectorAll('.slide'));
  const progress = document.getElementById('progress');
  let current = 0;

  // The background shader pans with navigation, following the fractional slide
  // position published below rather than the slide index. Publishing the index
  // alone left the shader to ease on a schedule of its own: it lunged off at
  // full speed and had all but settled while the slide, on an ease-in-out
  // curve, was barely under way. Here the pan is driven by the very curve the
  // slide travels on, read off the stylesheet so the two cannot drift apart.
  const slideStyle = getComputedStyle(slides[0]);
  const panDuration = (parseFloat(slideStyle.transitionDuration) || 0) * 1000;

  // A cubic Bézier easing, as CSS defines it: solve x(t) = progress by Newton,
  // then read y(t) off the same curve.
  const cubicBezier = (x1, y1, x2, y2) => {
    const at = (a, b, t) => ((1 - t) * 3 * ((1 - t) * a + t * b) + t * t) * t;
    const slopeAt = (a, b, t) =>
      3 * (1 - t) * (1 - t) * a + 6 * (1 - t) * t * (b - a) + 3 * t * t * (1 - b);
    return (x) => {
      let t = x;
      for (let i = 0; i < 8; i++) {
        const error = at(x1, x2, t) - x;
        const slope = slopeAt(x1, x2, t);
        if (Math.abs(error) < 1e-5 || Math.abs(slope) < 1e-6) break;
        t = Math.min(1, Math.max(0, t - error / slope));
      }
      return at(y1, y2, t);
    };
  };

  const bezierArgs = slideStyle.transitionTimingFunction.match(/cubic-bezier\(([^)]+)\)/);
  const panEasing = bezierArgs
    ? cubicBezier(...bezierArgs[1].split(',').map(Number))
    : (x) => x;

  let panFrom = 0, panTarget = 0, panStart = 0, panning = false;
  window.presentationScroll = 0;

  function panStep(now) {
    const progressed = panDuration > 0 ? Math.min(1, (now - panStart) / panDuration) : 1;
    window.presentationScroll = panFrom + (panTarget - panFrom) * panEasing(progressed);
    panning = progressed < 1;
    if (panning) requestAnimationFrame(panStep);
  }

  // Restarting mid-flight from the position reached so far, over the full
  // duration, is what the CSS transition does when the slide is switched again
  // before it lands.
  function panToSlide(index) {
    if (index === panTarget) return;
    panFrom = window.presentationScroll;
    panTarget = index;
    panStart = performance.now();
    if (!panning) {
      panning = true;
      requestAnimationFrame(panStep);
    }
  }

  // Build progress dots
  slides.forEach((_, i) => {
    const dot = document.createElement('span');
    dot.className = 'dot';
    dot.addEventListener('click', () => go(i));
    progress.appendChild(dot);
  });
  const dots = Array.from(progress.children);

  function go(index) {
    current = Math.max(0, Math.min(slides.length - 1, index));
    slides.forEach((s, i) => {
      s.classList.toggle('active', i === current);
      s.classList.toggle('past', i < current);
    });
    dots.forEach((d, i) => d.classList.toggle('active', i === current));
    location.hash = String(current + 1);
    panToSlide(current);
  }

  function next() { go(current + 1); }
  function prev() { go(current - 1); }

  document.getElementById('next').addEventListener('click', next);
  document.getElementById('prev').addEventListener('click', prev);

  // True fullscreen through the Fullscreen API: unlike the window-level full
  // screen of the OS, it drops the browser's tab strip, toolbar and bookmarks.
  // Escape exits it, as does pressing F again.
  const fullscreenButton = document.getElementById('fullscreen');
  const toggleFullscreen = () => {
    if (document.fullscreenElement) {
      document.exitFullscreen();
    } else {
      document.documentElement.requestFullscreen({ navigationUI: 'hide' }).catch(() => {});
    }
  };
  fullscreenButton.addEventListener('click', toggleFullscreen);
  document.addEventListener('fullscreenchange', () => {
    const on = !!document.fullscreenElement;
    fullscreenButton.setAttribute('aria-label', on ? 'Exit fullscreen' : 'Enter fullscreen');
    fullscreenButton.title = on ? 'Exit fullscreen (F)' : 'Fullscreen (F)';
  });

  document.addEventListener('keydown', (e) => {
    if (e.metaKey || e.ctrlKey || e.altKey) return;
    switch (e.key) {
      case 'f':
      case 'F':
        e.preventDefault(); toggleFullscreen(); break;
      case 'ArrowRight':
      case 'PageDown':
      case ' ':
        e.preventDefault(); next(); break;
      case 'ArrowLeft':
      case 'PageUp':
        e.preventDefault(); prev(); break;
      case 'Home':
        e.preventDefault(); go(0); break;
      case 'End':
        e.preventDefault(); go(slides.length - 1); break;
    }
  });

  // Touch navigation: a horizontal swipe advances the deck on mobile. The
  // vertical-dominance check leaves pinch-zoom pans and accidental scrolls alone.
  let touchX = null, touchY = null;
  document.addEventListener('touchstart', (e) => {
    touchX = e.changedTouches[0].clientX;
    touchY = e.changedTouches[0].clientY;
  }, { passive: true });
  document.addEventListener('touchend', (e) => {
    if (touchX === null) return;
    const dx = e.changedTouches[0].clientX - touchX;
    const dy = e.changedTouches[0].clientY - touchY;
    touchX = touchY = null;
    if (Math.abs(dx) > 40 && Math.abs(dx) > Math.abs(dy)) (dx < 0 ? next : prev)();
  }, { passive: true });

  // Print backdrop: the live WebGL shader is position:fixed and can't tile
  // across printed pages, so drop a captured frame behind every slide as a real
  // <img>. CSS background-images print unreliably in Chrome; <img> elements load
  // eagerly (even while display:none) and always print. A darkened variant
  // (background-print.jpg) is used so text stays legible on paper — the bright
  // background.jpg is kept for on-screen/social reference.
  slides.forEach((slide) => {
    const bg = document.createElement('img');
    bg.className = 'print-bg';
    bg.src = 'background-print.jpg';
    bg.alt = '';
    slide.insertBefore(bg, slide.firstChild);
  });

  // Side titles: the vertical heading runs the full height of the slide's
  // content box, whatever its wording. Type is measured at a reference size
  // and scaled to the available height, so the spine and the body beside it
  // share one extent. Re-run when the viewport or the webfont changes; skipped
  // in portrait, where the stylesheet lays the heading out horizontally again.
  const fitSideTitles = () => {
    document.querySelectorAll('.side-title > h2').forEach((h2) => {
      if (getComputedStyle(h2).writingMode !== 'vertical-rl') {
        h2.style.fontSize = '';
        return;
      }
      const slide = h2.parentElement;
      const style = getComputedStyle(slide);
      const available = slide.clientHeight
        - parseFloat(style.paddingTop) - parseFloat(style.paddingBottom);
      h2.style.fontSize = '100px';
      // Rotated a half-turn, the text run is the box height.
      const run = h2.getBoundingClientRect().height;
      if (run > 0) h2.style.fontSize = (100 * available / run) + 'px';
    });
  };
  fitSideTitles();
  window.addEventListener('resize', fitSideTitles);
  if (document.fonts) document.fonts.ready.then(fitSideTitles);

  // Deep-link / refresh support: #3 opens the third slide.
  const start = parseInt(location.hash.replace('#', ''), 10);
  go(Number.isFinite(start) && start > 0 ? start - 1 : 0);

  // Assemble email links from their parts so the plaintext address never
  // appears in the page source (basic spam-scraper obfuscation).
  document.querySelectorAll('a.email').forEach((a) => {
    const address = a.dataset.user + '@' + a.dataset.domain;
    a.href = 'mailto:' + address;
    a.textContent = address;
  });
})();