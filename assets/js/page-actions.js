/*
 * Copies the Markdown source of the page to the clipboard, for the page
 * actions rendered by PageLayout.kt: every element marked with
 * `data-copy-page` copies the source published at the `data-markdown`
 * path of the enclosing `.page-actions`, and the "Copy page" button
 * confirms it for a moment, like on https://platform.claude.com/docs/.
 */
(() => {

  const FEEDBACK_DURATION = 2000;

  /*
   * The source is fetched inside the ClipboardItem, so that the clipboard
   * write starts within the click, which Safari requires, while the
   * download is still in progress.
   */
  async function copyMarkdown(path) {
    const text = fetch(path).then(response => {
      if (!response.ok) throw new Error(`${path}: ${response.status}`);
      return response.text();
    });
    if (window.ClipboardItem) {
      await navigator.clipboard.write([
        new ClipboardItem({
          "text/plain": text.then(t => new Blob([t], {type: "text/plain"}))
        })
      ]);
    } else {
      await navigator.clipboard.writeText(await text);
    }
  }

  function showFeedback(button, icon, label) {
    const [iconElement, labelElement] = [button.querySelector("i"), button.querySelector("span")];
    const [previousIcon, previousLabel] = [iconElement.textContent, labelElement.textContent];
    iconElement.textContent = icon;
    labelElement.textContent = label;
    clearTimeout(button.feedbackTimeout);
    button.feedbackTimeout = setTimeout(() => {
      iconElement.textContent = previousIcon;
      labelElement.textContent = previousLabel;
    }, FEEDBACK_DURATION);
  }

  async function copyPage(trigger) {
    const actions = trigger.closest(".page-actions");
    const button = actions.querySelector(".page-copy");
    // closes the menu, which stays open only while it has the focus
    trigger.blur();
    try {
      await copyMarkdown(actions.dataset.markdown);
      showFeedback(button, "check", "Copied!");
    } catch (error) {
      console.error("Could not copy the page", error);
      showFeedback(button, "content_copy", "Copy failed");
    }
  }

  document.addEventListener("click", event => {
    const trigger = event.target.closest("[data-copy-page]");
    if (!trigger) return;
    // the menu item is a link to the source, followed only without script
    event.preventDefault();
    copyPage(trigger);
  });

})();
