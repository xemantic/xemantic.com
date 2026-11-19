/*
 * The page title, the `h1` this script follows, see PageLayout.kt, is
 * typed once, as if on a terminal, like the statements of the home page
 * (statements.js), and the block cursor stays blinking at its end.
 * The heading always holds the whole title, split into the typed and
 * the untyped part around the cursor, so that its layout is the one of
 * the finished title, and nothing moves while typing. The untyped part
 * is invisible, hence the `aria-label` keeping the title whole for
 * assistive technology.
 * The script runs right after the heading, before the first paint, so
 * the title is not shown complete before being typed. A title with
 * markup of its own, like the logo of the home page, is left alone,
 * and so is every title for a reader preferring reduced motion.
 */
(() => {

  const TYPE_DELAY = 75;
  // a long title is typed faster, in no more than this time
  const MAX_DURATION = 1500;

  const title = document.currentScript.previousElementSibling;
  if (!title || title.localName !== "h1" || title.children.length) return;
  if (matchMedia("(prefers-reduced-motion: reduce)").matches) return;
  const text = title.textContent.trim().replace(/\s+/g, " ");
  if (!text) return;

  const typed = document.createElement("span");
  const cursor = document.createElement("span");
  cursor.className = "typing-cursor";
  const untyped = document.createElement("span");
  untyped.className = "untyped";
  untyped.textContent = text;
  title.setAttribute("aria-label", text);
  title.replaceChildren(typed, cursor, untyped);

  const delay = Math.min(TYPE_DELAY, MAX_DURATION / text.length);
  let length = 0;

  /*
   * The rhythm of a keyboard is not even: most keys follow one another
   * quickly, a few take longer, a new word takes a moment, a new clause
   * a longer one, and now and then the typist hesitates.
   * Returns the factor of the delay before the key at the index.
   */
  function rhythm(text, index) {
    let factor = .45 + Math.random() * Math.random() * 2.2;
    if (text[index - 1] === " ") {
      factor += /[,.;:!?–—]/.test(text[index - 2]) ? 1.5 + Math.random() * 2 : Math.random() * .8;
    }
    if (Math.random() < .04) factor += 1.5 + Math.random() * 2.5;
    return factor;
  }

  function type() {
    typed.textContent = text.slice(0, length);
    untyped.textContent = text.slice(length);
    if (length === text.length) return title.classList.add("resting");
    setTimeout(type, delay * rhythm(text, length++));
  }

  type();

})();
