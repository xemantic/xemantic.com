/*
 * Loads a Google map, rendered from a `figure.google-map` of the Markdown
 * source (see ContentWidgets.kt), only once the reader asks for it:
 * the button of the consent panel is replaced with the map itself.
 * The page includes this script after every map, so it guards against
 * handling a click more than once.
 */
(() => {
  if (window.googleMapLoader) return;
  window.googleMapLoader = true;
  document.addEventListener("click", (event) => {
    const button = event.target.closest("button[data-google-map]");
    if (!button) return;
    const map = document.createElement("iframe");
    map.src = button.dataset.googleMap;
    map.title = button.dataset.title;
    map.allowFullscreen = true;
    map.referrerPolicy = "no-referrer-when-downgrade";
    button.closest(".google-map-consent").replaceWith(map);
  });
})();
