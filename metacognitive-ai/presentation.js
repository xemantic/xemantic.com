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

  // Deep-link / refresh support: #3 opens the third slide.
  const start = parseInt(location.hash.replace('#', ''), 10);
  go(Number.isFinite(start) && start > 0 ? start - 1 : 0);
})();