const GA_ID = "UA-35099425-1";
const cryptedEmail = "bssvpr@krznagvp.pbz";

const GA_DIST = "https://www.googletagmanager.com/gtag/js?" + GA_ID;

window.dataLayer = window.dataLayer || [];

function gtag(){
  dataLayer.push(arguments);
}

gtag("js", new Date());
gtag("config", GA_ID);

// we don't need to wait for this
addScript(GA_DIST);

const email = cryptedEmail.replace(/[a-zA-Z]/g, c => String.fromCharCode((c<="Z"?90:122)>=(c=c.charCodeAt(0)+13)?c:c-26));
document.querySelectorAll(".email").forEach(element => {
  if (element.nodeName == "A") {
    element.href = "mailto:" + email;
  } else if (element.nodeName = "SPAN") {
    element.appendChild(document.createTextNode(email));
  }
});

function initializeCustomProject(project) {
  if (project.id == "xemantic") {
    const iframe = project.querySelector("iframe");
    iframe.src = window.location.href.split("#")[0] + "?x";
  }
}

function cleanUpCustomProject(project) {
  if (project.id == "xemantic") {
    const iframe = project.querySelector("iframe");
    iframe.removeAttribute("src");
  }
}
