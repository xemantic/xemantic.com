const FADE_IN_DELAY = 7000;

const GA_ID = "UA-35099425-1";

const mainCss = "main.css";
const flickityDist = "https://unpkg.com/flickity@2/dist/flickity.pkgd.min.js";
const flickityCssDist = "https://unpkg.com/flickity@2/dist/flickity.min.css";
const flickityBgLazyLoadDist = "https://npmcdn.com/flickity-bg-lazyload@1.0.0/bg-lazyload.js";
const flickityFullscreenDist = "https://unpkg.com/flickity-fullscreen@1/fullscreen.js";
const flickityFullscreenCssDist = "https://unpkg.com/flickity-fullscreen@1/fullscreen.css";

const gaDist = "https://www.googletagmanager.com/gtag/js?" + GA_ID;
const vimeoDist = "https://player.vimeo.com/api/player.js";
const arsenalFontDist = "https://fonts.googleapis.com/css2?family=Arsenal:ital,wght@0,400;0,700;1,400;1,700&display=swap";
const fontAwesomeDist = "https://kit.fontawesome.com/0bbdfd8b39.js";

const cryptedEmail = "znvygb:bssvpr@krznagvp.pbz";

function loaded(element) {
  element.classList.add("loaded");
}

// restoring back image src attributes
for (let i = 0; i < document.images.length; i++) {
  let image = document.images[i];
  let dataSrc = image.getAttribute("data-src");
  if (dataSrc) {
    image.onload = loaded(image);
    image.src = image.getAttribute("data-src");
    image.removeAttribute("data-src");
  }
}

const miniatures = document.getElementById("project-miniatures");
const projects = document.getElementById("projects");
const me = document.getElementById("me");
const footer = document.getElementsByTagName("footer")[0];

function show(element) {
  element.classList.remove("hidden");
  element.classList.add("fade-in");
}

//let email = cryptedEmail.replace(/[a-zA-Z]/g, function(c){return String.fromCharCode((c<="Z"?90:122)>=(c=c.charCodeAt(0)+13)?c:c-26);});
//document.querySelectorAll(".email").forEach(element => {
//  element.appendChild(email);
//})

window.dataLayer = window.dataLayer || [];

function gtag(){
  dataLayer.push(arguments);
}

gtag("js", new Date());
gtag("config", GA_ID);

function addCss(href, callback, errorCallback) {
  const link = document.createElement("link");
  if (callback) {
    link.onload = callback;
  }
  if (errorCallback) {
    link.onerror = errorCallback;
  };
  link.setAttribute("href", href);
  link.setAttribute("rel", "stylesheet");
  document.head.appendChild(link);
}

const loadJs = (src, crossorigin) => {
  return new Promise((resolve, reject) => {
    addScript(src, () => { resolve(); }, (error) => { reject(error); }, crossorigin);
  });
};

const loadCss = (src) => {
  return new Promise((resolve, reject) => {
    addCss(src, () => {
    resolve(); }, (error) => { reject(error); });
  });
};

Promise.all([
  loadCss(arsenalFontDist),
  loadJs(fontAwesomeDist, "anonymous"),
  loadJs(flickityDist).then(() => Promise.all([
    loadJs(flickityFullscreenDist),
    loadJs(flickityBgLazyLoadDist)
  ])),
  loadCss(mainCss),
  loadCss(flickityCssDist),
  loadCss(flickityFullscreenCssDist)
]).then(() => {
  const flickityMiniatures = new Flickity(miniatures, {
    bgLazyLoad: 2,
    freeScroll: true,
    pageDots: false,
  });
  const hashChangeHandler = (e) => {
    let hash = location.hash;
    let project = projects.querySelector(hash)
    if (project) {
      flickityMiniatures.selectCell("[href='" + hash + "']");
      let height = project.clientHeight + "px";
      projects.style.height = height;
      me.style.marginTop = height;
    }
  };
  window.addEventListener("hashchange", hashChangeHandler, false);

  const fadeInTime = START_TIME + FADE_IN_DELAY - Date.now();
  setTimeout(() => {
    root.style.setProperty("--z-index-background", -2);
    show(miniatures);
    show(projects);
    show(me);
    show(footer);
    flickityMiniatures.resize();
  }, fadeInTime);

});

// we don't need to wait for these
addScript(gaDist);
addScript(vimeoDist);
