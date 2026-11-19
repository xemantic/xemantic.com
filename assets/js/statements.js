/*
 * The statements widget rendered by ContentWidgets.kt, a `div.statements`
 * holding a list: the statements are typed one at a time, as if on
 * a terminal, a block cursor following the text, then deleted and
 * replaced by the next one, in a loop.
 * The list is left as it is, for xemantic.css to keep in place unseen;
 * the typing happens in a line of its own, hidden from assistive
 * technology, which reads the list instead. The line always holds the
 * whole statement, split into the typed and the untyped part around
 * the cursor, so that its layout is the one of the finished statement.
 * A reader preferring reduced motion gets the statements replaced
 * as a whole, with no typing.
 */
(() => {

  const TYPE_DELAY = 75;
  const DELETE_DELAY = 35;
  const REST = 2200;
  const PAUSE = 450;

  const reducedMotion = matchMedia("(prefers-reduced-motion: reduce)");

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

  document.querySelectorAll("div.statements").forEach(widget => {
    const statements = [...widget.querySelectorAll(":scope > ul > li")]
      .map(statement => statement.textContent.trim().replace(/\s+/g, " "))
      .filter(Boolean);
    if (!statements.length) return;

    const line = document.createElement("p");
    // BeerCSS spares an element with a margin class its 1rem top margin of a block following a sibling
    line.className = "statement-line no-margin";
    line.setAttribute("aria-hidden", "true");
    const typed = document.createElement("span");
    const cursor = document.createElement("span");
    cursor.className = "typing-cursor";
    const untyped = document.createElement("span");
    untyped.className = "untyped";
    line.append(typed, cursor, untyped);
    widget.append(line);

    let index = 0;
    let length = 0;

    function show() {
      typed.textContent = statements[index].slice(0, length);
      untyped.textContent = statements[index].slice(length);
    }

    function rest() {
      line.classList.add("resting");
      setTimeout(remove, REST);
    }

    function type() {
      line.classList.remove("resting");
      if (reducedMotion.matches) length = statements[index].length;
      show();
      if (length === statements[index].length) return rest();
      setTimeout(type, TYPE_DELAY * rhythm(statements[index], length++));
    }

    function remove() {
      line.classList.remove("resting");
      if (reducedMotion.matches) length = 0;
      show();
      if (length === 0) {
        index = (index + 1) % statements.length;
        return setTimeout(type, reducedMotion.matches ? 0 : PAUSE);
      }
      length--;
      // a held backspace repeats almost evenly
      setTimeout(remove, DELETE_DELAY * (.85 + Math.random() * .3));
    }

    type();
  });

})();
