const GA_ID = "UA-35099425-1";

const mainCss = "main.css";
const flickityDist = "https://unpkg.com/flickity@2/dist/flickity.pkgd.min.js";
const flickityCssDist = "https://unpkg.com/flickity@2/dist/flickity.min.css";
const flickityBgLazyLoadDist = "https://npmcdn.com/flickity-bg-lazyload@1.0.0/bg-lazyload.js";
const gaDist = "https://www.googletagmanager.com/gtag/js?" + GA_ID;
const vimeoDist = "https://player.vimeo.com/api/player.js";
const arsenalFontDist = "https://fonts.googleapis.com/css2?family=Arsenal:ital,wght@0,400;0,700;1,400;1,700&display=swap";
const fontAwesomeDist = "https://kit.fontawesome.com/0bbdfd8b39.js";

const cryptedEmail = "znvygb:bssvpr@krznagvp.pbz";

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
  var link = document.createElement("link");
    if (callback) {
      link.onload = callback;
    }
    if (errorCallback) {
      link.onerror = errorCallback;
    }
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
    console.log("resolved: " + src);
    resolve(); }, (error) => { reject(error); });
  });
};

Promise.all([
  loadCss(arsenalFontDist),
  loadJs(fontAwesomeDist, "anonymous"),
  loadJs(flickityDist).then(() => loadJs(flickityBgLazyLoadDist)),
  loadCss(mainCss),
  loadCss(flickityCssDist)
]).then(() => {
  document.getElementById("hider").remove(); // show the whole content
  const projects = document.getElementById("projects");
  const miniatures = document.getElementById("project-miniatures");
  var flickityMiniatures = new Flickity(miniatures, {
    bgLazyLoad: 2,
    freeScroll: true,
    pageDots: false,
  });
  window.addEventListener("hashchange", e => {
    var hash = location.hash;
    if (projects.querySelector(hash)) { // one of the projects
      flickityMiniatures.selectCell("[href='" + hash + "']");
    }
  }, false); // TODO what's the mining of false here?
});

// we don't need to wait for these
addScript(gaDist);
addScript(vimeoDist);
