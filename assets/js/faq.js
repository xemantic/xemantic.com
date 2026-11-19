/*
 * Unfolds the question of a `div.faq` (see ContentWidgets.kt) a link
 * points to, like those of the table of contents: the `id` of a question
 * is the one of its `details`, which the browser scrolls to, but does not
 * open by itself. The page includes this script after every list of
 * questions, so it guards against being set up more than once.
 */
(() => {
  if (window.faqUnfolder) return;
  window.faqUnfolder = true;
  const unfold = () => {
    if (location.hash.length < 2) return;
    const target = document.getElementById(decodeURIComponent(location.hash.slice(1)));
    const question = target?.closest(".faq > details");
    if (!question) return;
    question.open = true;
    question.scrollIntoView();
  };
  window.addEventListener("hashchange", unfold);
  unfold();
})();
