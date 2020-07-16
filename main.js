const IFRAME_REMOVAL_DELAY = 3000;

const GA_ID = "UA-35099425-1";

const MAIN_CSS = "main.css";
const FLICKITY_DIST = "https://unpkg.com/flickity@2/dist/flickity.pkgd.min.js";
const FLICKITY_CSS_DIST = "https://unpkg.com/flickity@2/dist/flickity.min.css";
const FLICKITY_BG_LAZY_LOAD_DIST = "https://npmcdn.com/flickity-bg-lazyload@1.0.0/bg-lazyload.js";
const FLICKITY_FULLSCREEN_DIST = "https://unpkg.com/flickity-fullscreen@1/fullscreen.js";
const FLICKITY_FULLSCREEN_CSS_DIST = "https://unpkg.com/flickity-fullscreen@1/fullscreen.css";

const GA_DIST = "https://www.googletagmanager.com/gtag/js?" + GA_ID;
const VIMEO_DIST = "https://player.vimeo.com/api/player.js";
const FONT_DIST = "https://fonts.googleapis.com/css2?family=Arsenal:ital,wght@0,400;0,700;1,400;1,700&display=swap";
const FONT_AWESOME_DIST = "https://kit.fontawesome.com/0bbdfd8b39.js";

const cryptedEmail = "bssvpr@krznagvp.pbz";

let email = cryptedEmail.replace(/[a-zA-Z]/g, c => String.fromCharCode((c<="Z"?90:122)>=(c=c.charCodeAt(0)+13)?c:c-26));
document.querySelectorAll(".email").forEach(element => {
  if (element.nodeName == "A") {
    element.href = "mailto:" + email;
  } else if (element.nodeName = "SPAN") {
    element.appendChild(document.createTextNode(email));
  }
});

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

const loadJs = (src, crossorigin) => new Promise((resolve, reject) => {
  addScript(src, () => { resolve(); }, (error) => { reject(error); }, crossorigin);
});

const loadCss = (src) => new Promise((resolve, reject) => {
  addCss(src, () => { resolve(); }, (error) => { reject(error); });
});

function newPlayer(element) {
  element.player = new Vimeo.Player(element, {
    id: element.getAttribute("data-video-id"),
    responsive: true,
    byline: false,
    title: false,
    color: "ffffff"
  });
  return element.player;
}

function initializeMedia(media) {
  media.querySelectorAll(":scope > .flickity").forEach(flickityElement => {
    const flickity = new Flickity(flickityElement, {
      adaptiveHeight: true
    });
    flickityElement.flickity = flickity;
    flickityElement.querySelectorAll(".vimeo-video").forEach(video => {
      const player = newPlayer(video);
      function showButtons(visibility) {
        const value = visibility ? "visible" : "hidden";
        flickity.prevButton.element.style.visibility = value;
        flickity.nextButton.element.style.visibility = value;
      }
      player.ready().then(() => flickity.resize());
      player.on("playing", () => flickity.showButtons(false));
      player.on("pause",   () => flickity.showButtons(true));
      player.on("ended",   () => flickity.showButtons(true));
      player.on("loaded",  () => video.classList.add("loaded"));
      flickity.on("change", (index) => { player.pause(); })
    });
  });
  media.querySelectorAll(":scope > .vimeo-video").forEach(video => {
    newPlayer(vimeo);
  });
}

function cleanUpMedia(media) {
  videos = media.querySelectorAll(".vimeo-video");
  videos.forEach(video => {
    if (video.player) {
      video.player.destroy().then(() => {
        video.player = null;
        video.textContent = "";
      });
    }
    video.classList.remove("loaded");
  });
  const flickity = media.querySelector(".flickity");
  if (flickity) {
    flickity.flickity.destroy();
  }
}

function initializeProject(project) {
  if (project.id == "xemantic") {
    const iframe = project.querySelector("iframe");
    iframe.src = window.location.href.split("#")[0] + "?x"
  } else {
    initializeMedia(project.querySelector(".media"));
  }
}

function cleanUpProject(project) {
  if (project.id == "xemantic") {
    let iframe = project.querySelector("iframe");
    iframe.removeAttribute("src");
  } else {
    cleanUpMedia(project.querySelector(".media"));
  }
}

Promise.all([
  loadCss(FONT_DIST),
  loadJs(FONT_AWESOME_DIST, "anonymous"),
  loadJs(FLICKITY_DIST).then(() => Promise.all([
    loadJs(FLICKITY_FULLSCREEN_DIST),
    loadJs(FLICKITY_BG_LAZY_LOAD_DIST)
  ])),
  loadJs(VIMEO_DIST),
  loadCss(MAIN_CSS),
  loadCss(FLICKITY_CSS_DIST),
  loadCss(FLICKITY_FULLSCREEN_CSS_DIST)
]).then(() => {
  const flickityMiniatures = new Flickity(miniatures, {
    bgLazyLoad: 2,
    freeScroll: true,
    pageDots: false,
  });
  var selectedProject = null;
  const projectCleaner = (projectToClean) => {
    if (!projectToClean) {
      return;
    }
    setTimeout(() => {
      if (projectToClean != selectedProject) {
        cleanUpProject(projectToClean);
      }
    }, IFRAME_REMOVAL_DELAY);
  }

  const hashChangeHandler = (e) => {
    const hash = location.hash;
    if (!hash) return;
    const project = projects.querySelector(hash);
    if (!project) return;
    flickityMiniatures.selectCell("[href='" + hash + "']");
    const height = project.clientHeight + "px";
    projects.style.height = height;
    me.style.marginTop = height;
    initializeProject(project);
    projectCleaner(selectedProject);
    selectedProject = project;
  };
  window.addEventListener("hashchange", hashChangeHandler, false);

  root.style.setProperty("--z-index-background", -2);
  show(miniatures);
  show(projects);
  show(me);
  show(footer);
  flickityMiniatures.resize();
  let hash = window.location.hash;
  if (hash) {
    hashChangeHandler();
    if (selectedProject) {
      selectedProject.scrollIntoView();
    }
  }
});

// we don't need to wait for this
addScript(GA_DIST);
