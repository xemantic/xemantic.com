(() => {
  let scheme;
  try {
    scheme = localStorage.getItem("color-scheme");
  } catch (error) {}
  if (scheme !== "dark" && scheme !== "light") {
    scheme = matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light";
  }
  document.body.classList.add(scheme);
})();
