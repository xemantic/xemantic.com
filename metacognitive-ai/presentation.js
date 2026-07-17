(function () {
  'use strict';
  const slides = Array.from(document.querySelectorAll('.slide'));
  const progress = document.getElementById('progress');
  let current = 0;

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
    // Publish the active slide index so the background shader can pan with navigation.
    window.presentationScroll = current;
  }

  function next() { go(current + 1); }
  function prev() { go(current - 1); }

  document.getElementById('next').addEventListener('click', next);
  document.getElementById('prev').addEventListener('click', prev);

  document.addEventListener('keydown', (e) => {
    switch (e.key) {
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