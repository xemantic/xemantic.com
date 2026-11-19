/*
 * The color scheme switch of the top app bar rendered by PageLayout.kt:
 * every element marked with `data-toggle-color-scheme` swaps BeerCSS's
 * `dark` and `light` classes of `body`, and the choice is remembered, to
 * be applied before the first paint of the next page by the inline
 * script at the start of `body`. Until a choice is made, the pages
 * follow the color scheme preference of the browser.
 */
(() => {

  document.addEventListener("click", event => {
    if (!event.target.closest("[data-toggle-color-scheme]")) return;
    const scheme = document.body.classList.contains("dark") ? "light" : "dark";
    document.body.classList.remove("dark", "light");
    document.body.classList.add(scheme);
    try {
      localStorage.setItem("color-scheme", scheme);
    } catch (error) {
      // no storage, e.g. in a private window: the choice lasts for this page
    }
  });

})();
