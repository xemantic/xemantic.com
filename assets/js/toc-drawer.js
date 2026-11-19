/*
 * The table of contents drawer rendered by PageLayout.kt: a BeerCSS
 * `dialog.left` opened and closed by the toggle button of the top app
 * bar through the Popover API. Without this script it is an ordinary
 * `auto` popover, closed until opened. On viewports wide enough to fit
 * it next to the content, the script turns it into a `manual` popover,
 * open by default and not closed by a click on the content, while on
 * narrower ones it stays an `auto` popover, closed by a click outside,
 * by Escape, and by following one of its links. Crossing the breakpoint
 * restores the default of the new viewport. A page without a table of
 * contents, whose drawer holds the page actions alone, or one setting `toc: closed`
 * in its front matter (`data-default="closed"` on the drawer) keeps the
 * drawer closed on wide viewports as well, until the toggle is clicked,
 * and then it opens next to the content just the same. The script runs right after
 * the drawer, before the first paint, so a wide page does not show the
 * drawer sliding in. Following a link of the table of contents copied
 * to the navigation drawer closes that one as well.
 */
(() => {

  const drawer = document.currentScript.previousElementSibling;
  const wide = matchMedia("(min-width: 80rem)");
  const closed = drawer.dataset.default === "closed";

  function applyDefault() {
    if (drawer.matches(":popover-open")) drawer.hidePopover();
    drawer.popover = wide.matches ? "manual" : "auto";
    if (wide.matches && !closed) {
      drawer.style.transition = "none";
      drawer.showPopover();
      // showing a dialog focuses its first link, unwanted when not asked for
      document.activeElement.blur();
      // the style is applied without the transition before restoring it
      drawer.offsetWidth;
      drawer.style.transition = "";
    }
  }

  applyDefault();
  wide.addEventListener("change", applyDefault);

  drawer.addEventListener("click", event => {
    if (drawer.popover === "auto" && event.target.closest("a")) {
      drawer.hidePopover();
    }
  });

  // the copy of the table of contents in the navigation drawer
  const menu = document.getElementById("menu");
  menu.addEventListener("click", event => {
    if (event.target.closest(".toc a")) menu.hidePopover();
  });

  /*
   * Scroll spy: the link of the section being read, the last one whose
   * top has scrolled up to its scroll margin, below the top app bar, is
   * marked with `aria-current`, and the indicator bar drawn by
   * xemantic.css next to the table of contents is moved to it, through
   * the `--toc-indicator-*` properties. A section too short to ever
   * reach the top is still marked when its link was followed, until the
   * reader scrolls on their own.
   */
  const navs = [...document.querySelectorAll("nav#toc, nav.toc")];
  const links = navs.flatMap(nav => [...nav.querySelectorAll("a[href^='#']")]);
  const targets = [...new Set(links.map(link => link.hash))]
    .map(hash => document.getElementById(decodeURIComponent(hash.slice(1))))
    .filter(Boolean);
  let followed = null;
  const reducedMotion = matchMedia("(prefers-reduced-motion: reduce)");

  function current() {
    if (followed) return followed;
    const atBottom = innerHeight + scrollY >= document.documentElement.scrollHeight - 1;
    if (atBottom) return targets[targets.length - 1];
    // above the first section, its link is marked anyway
    let reading = targets[0];
    for (const target of targets) {
      const margin = parseFloat(getComputedStyle(target).scrollMarginBlockStart) || 0;
      if (target.getBoundingClientRect().top - margin > 1) break;
      reading = target;
    }
    return reading;
  }

  function update() {
    const hash = current() ? "#" + current().id : null;
    navs.forEach(nav => {
      const list = nav.querySelector(":scope > ul");
      let active = null;
      nav.querySelectorAll("a[href^='#']").forEach(link => {
        if (decodeURIComponent(link.hash) === hash) {
          link.setAttribute("aria-current", "location");
          active = link;
        } else {
          link.removeAttribute("aria-current");
        }
      });
      if (!list) return;
      if (!active || !active.offsetParent) {
        list.style.setProperty("--toc-indicator-size", "0");
        return;
      }
      const offset = active.getBoundingClientRect().top - list.getBoundingClientRect().top;
      list.style.setProperty("--toc-indicator-offset", offset + "px");
      list.style.setProperty("--toc-indicator-size", active.offsetHeight + "px");
      // keeping the active link visible in a table of contents taller
      // than the drawer, which scrolls it, not the page like scrollIntoView
      if (nav.scrollHeight > nav.clientHeight) {
        const top = active.getBoundingClientRect().top - nav.getBoundingClientRect().top + nav.scrollTop;
        if (top < nav.scrollTop || top + active.offsetHeight > nav.scrollTop + nav.clientHeight) {
          nav.scrollTo({
            top: top - nav.clientHeight / 3,
            behavior: reducedMotion.matches ? "instant" : "smooth"
          });
        }
      }
    });
  }

  /*
   * A table of contents taller than the drawer scrolls within it, and
   * xemantic.css fades it out towards the edge it continues beyond,
   * given the distance left to scroll in either direction.
   */
  const scrolled = drawer.querySelector("nav#toc");

  function fade() {
    if (!scrolled) return;
    const before = scrolled.scrollTop;
    const after = scrolled.scrollHeight - scrolled.clientHeight - before;
    scrolled.style.setProperty("--toc-scroll-before", Math.max(0, before) + "px");
    scrolled.style.setProperty("--toc-scroll-after", Math.max(0, after) + "px");
  }

  if (scrolled) {
    scrolled.addEventListener("scroll", fade, { passive: true });
    // neither a font loaded late, rewrapping the links, nor the drawer
    // being shown, fires any scroll or resize event
    const resized = new ResizeObserver(fade);
    resized.observe(scrolled);
    [...scrolled.children].forEach(child => resized.observe(child));
  }

  let scheduled = false;
  function schedule() {
    if (scheduled) return;
    scheduled = true;
    requestAnimationFrame(() => {
      scheduled = false;
      update();
      fade();
    });
  }

  links.forEach(link => link.addEventListener("click", () => {
    followed = document.getElementById(decodeURIComponent(link.hash.slice(1)));
    schedule();
  }));
  ["wheel", "touchstart", "keydown"].forEach(type =>
    addEventListener(type, () => { followed = null; }, { passive: true })
  );
  addEventListener("scroll", schedule, { passive: true });
  addEventListener("resize", schedule);
  // a hidden table of contents has no layout to place the indicator in
  drawer.addEventListener("toggle", schedule);
  menu.addEventListener("toggle", schedule);
  update();
  fade();

})();
