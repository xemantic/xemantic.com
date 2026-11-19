/*
 * Drives the generative art deco background, the shader of
 * assets/shaders/generative-art-deco.frag. shader-web-background can only read
 * shaders included in the HTML, so the shader is fetched, and put into the
 * document as the `image` script, before the shading starts - keeping
 * the source of every page free of it.
 * The pan follows the scrolling of the page, the same way the background of
 * the consciousness presentation follows its slides, and the pointer changes
 * the refraction of the glass under it.
 *
 * Every coefficient of the shader is set here, and everything changing with time
 * is computed here once per frame, to be supplied to the shader as a uniform.
 * The parameters are read anew on every frame, and exposed as
 * window.generativeArtDeco, so that assets/shaders/generative-art-deco.html
 * can tune them live.
 */
(() => {

  // resolved against this script, so that the playground in assets/shaders works too
  const SHADER_URL = new URL("../shaders/generative-art-deco.frag", document.currentScript.src);

  const PARALLAX_SCROLLING_RATE = .0003; // shader units panned per device pixel scrolled

  // An oscillator describes a value swinging around its center:
  // center + amplitude * sin(time * rate + phase).
  const oscillate = ({center, amplitude, rate, phase}, t) =>
    center + amplitude * Math.sin(t * rate + phase);

  const parameters = {
    timeScale: .0001,              // time units per millisecond

    // the cell
    waveFrequency: 3,              // how many rings the wave of the cell makes
    shapeSize: .618,               // where the wave is cut into a shape, the golden ratio

    // the camera sways back and forth, and slowly breathes in and out
    cameraAngle: {center: 0, amplitude: .3, rate: .14, phase: 0},
    cameraZoom: {center: .6, amplitude: .3, rate: .15, phase: 0},

    // the lens, see the shader
    lensScale: .428,
    lensStrength: 1.1,

    // the journey through the infinite plane, a slow ellipse detuned into a Lissajous curve
    drift: [
      {center: 0, amplitude: 1, rate: .12, phase: 0},
      {center: 0, amplitude: 1, rate: .13, phase: Math.PI / 2},
    ],

    chromaticAberration: .01,

    // the pointer thickens the glass under it, see the shader
    pointerRadius: .4,             // how far its influence reaches, in screen space
    pointerRefraction: 1.5,        // the magnification it adds to the layers below
    pointerDispersion: .005,       // the chromatic aberration it adds
    pointerStiffness: 30,          // of the spring it is trailed on, the higher the tighter
    pointerFade: 2,                // how quickly it fades in and out, per second

    // depth of field: the blur of the top layer pulses, the focus hunting for a subject
    blur: {center: .4, amplitude: .2, rate: .52, phase: 0},
    blurFalloff: .63,              // how much of its blur every deeper layer keeps
    // aerial perspective
    initialLuma: .5,               // the brightness of the top layer
    lumaFalloff: .6,               // how much of it every deeper layer keeps

    // descending into the next layer
    layerZoom: 1.1,                // outside of the shape of the layer above
    layerMagnification: 1.2,       // added where seen through the shape
    layerEdge: .04,                // the softness of the edge between the two
    layerTurn: {center: 0, amplitude: 1.33, rate: .05, phase: 0},

    // Color grading: every channel gets its own gamma, drifting with time,
    // with the inner and the outer grading detuned in frequency.
    // It cycles the colors of the dark color scheme only: against a lit sky
    // it is barely visible, so it fades out together with the darkness.
    grading: {
      intensity: .4,
      rate: .3,
      polarity: [1, 1, -1],        // blue swings against red and green
      innerDetune: [1.13, 1.23, 1.33],
      outerDetune: [1.43, 1.53, 1.63],
    },
    gradingRadius: .5,

    // The sky behind the deepest layer, see the shader: black in the dark
    // color scheme, of the light tone of the primary color of the theme
    // in the light one - `--inverse-primary` there, the primary color of
    // the dark scheme, as the primary color itself makes too dark a sky.
    // This color stands in where no theme defines it, as in the playground.
    sky: [.45, .68, .95],
    // A lit sky leaves the light of the layers little room before it burns out,
    // so against it they shine with only this much of their brightness.
    skyLuma: .3,
    skyRim: .25,                   // how much of the sky the rim of a shape bends away, see the shader
    skyFade: 3,                    // how quickly it follows a change of the scheme, per second

    // The column of text of an article (`div.prose`, see ArticleLayout.kt):
    // behind it the picture fades towards the color of the tinted glass
    // surfaces of the page, see xemantic.css, which leaves the text a calm
    // background to be read against, in either color scheme.
    columnVeil: .93,               // how much of the picture gives way to that color there
    columnTint: .12,               // how much of the primary color tints the surface color, as in xemantic.css
    // This color stands in where no theme defines it, as in the playground.
    columnColor: [.1, .09, .12],
    columnFeather: 1.2,            // the width of its soft edge, as a fraction of the column
    // The veil ends where the text does: above the first run of the text,
    // and below the last one, it fades away, leaving the top
    // and the bottom of the page to the picture.
    columnFade: .5,                // the height of that fade, as a fraction of the column
  };

  const gamma = (detune, sign, t) => detune.map((d, i) =>
    1 + sign * Math.sin(t * parameters.grading.rate * d)
      * parameters.grading.intensity * (1 - daylight) * parameters.grading.polarity[i]
  );

  // The light tone of the primary color of the theme, as the channels of the sky,
  // or undefined if there is none. Painted on a canvas, to be read back as numbers,
  // whichever CSS notation the theme defines it in.
  const probe = document.createElement("canvas").getContext("2d", {willReadFrequently: true});
  probe.canvas.width = probe.canvas.height = 1;
  const channels = (color) => {
    if (!color || !CSS.supports("color", color)) return undefined;
    probe.fillStyle = color;
    probe.fillRect(0, 0, 1, 1);
    return [...probe.getImageData(0, 0, 1, 1).data.slice(0, 3)].map((channel) => channel / 255);
  };
  const themeColor = (name) => getComputedStyle(document.body).getPropertyValue(name).trim();
  const skyColor = () => channels(themeColor("--inverse-primary"));

  // The color of the tinted glass surfaces of the page, the `tinted-glass`
  // of xemantic.css without its transparency, or undefined if no theme defines it.
  const glassColor = () => {
    const primary = themeColor("--primary");
    const surface = themeColor("--surface");
    return primary && surface
      ? channels(`color-mix(in oklab, ${primary} ${parameters.columnTint * 100}%, ${surface})`)
      : undefined;
  };

  // Mirrors rotate() of the shader, so that a step taken in screen space can be
  // pushed into the frame a layer is drawn in. Keep in sync with the GLSL.
  const rotate = ([x, y], a) => {
    const c = Math.cos(a);
    const s = Math.sin(a);
    return [c * x - s * y, s * x + c * y];
  };

  // accumulated, so that a change of the time scale does not make it jump
  let time = Math.random() * 100;
  let lastNow = performance.now();
  let scroll = 0;
  let cameraAngle, cameraZoom, drift, blur, layerTurn, innerGamma, outerGamma;
  // how much of the sky is lit, from 0 in the dark color scheme to 1 in the
  // light one, unknown until the first frame, which does not fade
  let daylight;
  let day;
  // the sky of the light color scheme, read from the theme whenever the day breaks
  let themeSky;
  // The pan is accumulated in the pattern space of every layer, instead of being
  // transformed into it anew on each frame. A stored offset is a standing
  // translation, so once the page stops scrolling it stops changing, and the bottom
  // of a long page animates at the rate of its top - a pan derived from the scroll
  // position would instead keep being swung around by the rotation of the scene, ever
  // faster the further it reaches from the center it revolves around. Only fract(p)
  // ever reaches the screen, so wrapping the offsets to a single period is free.
  const offsets = [[0, 0], [0, 0], [0, 0]];

  // The pointer, in the screen space of the shader: centered, the shorter side
  // spanning 1, y pointing up. Its influence fades out while it is outside of
  // the window, and stays out on touch screens, which have no pointer hovering
  // over the scene, and when the reader asks for reduced motion.
  const pointer = [0, 0];
  const pointerTrail = [0, 0];
  const pointerVelocity = [0, 0];
  let pointerInside = false;
  let presence = 0;
  const tracking = matchMedia(
    "(hover: hover) and (pointer: fine) and (prefers-reduced-motion: no-preference)"
  );
  addEventListener("pointermove", (event) => {
    if (!tracking.matches) return;
    const size = Math.min(innerWidth, innerHeight);
    pointer[0] = (event.clientX - innerWidth / 2) / size;
    pointer[1] = (innerHeight / 2 - event.clientY) / size;
    pointerInside = true;
  });
  const leave = () => pointerInside = false;
  addEventListener("pointerout", (event) => { if (!event.relatedTarget) leave(); });
  addEventListener("blur", leave);
  tracking.addEventListener("change", leave);
  let uniforms;

  // The column of text, if the page has any: its center and half of its width,
  // in device pixels, measured on every frame, as it moves with the layout,
  // e.g. when the table of contents opens next to it.
  let prose;
  const column = [0, 0];
  // where the text ends and where it starts, the bottom of its last run
  // and the top of the first one, in device pixels counted from the bottom
  // of the canvas, like gl_FragCoord.y
  const columnSpan = [0, 0];
  // the color the picture fades to behind it, following the color scheme like the sky does
  let columnColor;
  let columnTarget;

  // shader-web-background reads the shader synchronously, as soon as shade() is
  // called, from a script of the document.
  const insertShader = (source) => {
    const script = document.createElement("script");
    script.type = "x-shader/x-fragment";
    script.id = "image";
    script.text = source;
    document.head.append(script);
  };

  // Left to itself, shader-web-background sizes its canvas on the load event, and
  // only then adds it to the document, so called after the load, which the fetch
  // of the shader can easily outlast, shade() would leave the canvas 0 by 0 pixels.
  // A canvas already in the document is sized right, whenever shade() is called.
  const createCanvas = () => {
    const canvas = document.createElement("canvas");
    canvas.id = "shader-web-background";
    Object.assign(canvas.style, {
      width: "100vw",
      height: "100vh",
      position: "fixed",
      top: "0",
      left: "0",
      zIndex: "-9999",
    });
    document.body.append(canvas);
    return canvas;
  };

  const domReady = new Promise((resolve) => {
    if (document.readyState === "loading") {
      addEventListener("DOMContentLoaded", resolve, {once: true});
    } else {
      resolve();
    }
  });

  const shade = () => shaderWebBackground.shade((ctx) => ({
    canvas: createCanvas(),
    onBeforeFrame: () => {
      const now = performance.now();
      const elapsed = now - lastNow;
      time += elapsed * parameters.timeScale;
      lastNow = now;
      const p = parameters;

      // Real time, unaffected by the time scale. The step is capped, so that
      // a frame arriving late, e.g. in a tab coming back from the background,
      // cannot throw the integration off balance.
      const dt = Math.min(elapsed / 1000, 1 / 30);
      presence += ((pointerInside ? 1 : 0) - presence) * (1 - Math.exp(-p.pointerFade * dt));
      if (presence < .01) {
        // faded out, it reappears right where the pointer comes back
        pointerTrail.splice(0, 2, ...pointer);
        pointerVelocity.fill(0);
      }
      // Critically damped: the trail catches up with the pointer as fast
      // as it can without overshooting.
      const damping = 2 * Math.sqrt(p.pointerStiffness);
      for (let i = 0; i < 2; i++) {
        pointerVelocity[i] += (p.pointerStiffness * (pointer[i] - pointerTrail[i]) - damping * pointerVelocity[i]) * dt;
        pointerTrail[i] += pointerVelocity[i] * dt;
      }
      // BeerCSS's class of the body, swapped by assets/js/color-scheme.js
      const wasDay = day;
      day = document.body.classList.contains("light") ? 1 : 0;
      if (day && !wasDay) themeSky = skyColor();
      if (day !== wasDay) {
        const glass = glassColor() ?? p.columnColor;
        columnColor ??= [...glass];
        columnTarget = glass;
      }
      const follow = 1 - Math.exp(-p.skyFade * dt);
      columnColor.forEach((channel, i) => columnColor[i] = channel + (columnTarget[i] - channel) * follow);
      daylight = daylight === undefined
        ? day
        : daylight + (day - daylight) * (1 - Math.exp(-p.skyFade * dt));
      cameraAngle = oscillate(p.cameraAngle, time);
      cameraZoom = oscillate(p.cameraZoom, time);
      drift = p.drift.map((axis) => oscillate(axis, time));
      blur = oscillate(p.blur, time);
      layerTurn = oscillate(p.layerTurn, time);
      innerGamma = gamma(p.grading.innerDetune, 1, time);
      outerGamma = gamma(p.grading.outerDetune, -1, time);

      if (prose === undefined) {
        const runs = document.querySelectorAll("main .prose");
        prose = runs.length ? [runs[0], runs[runs.length - 1]] : false;
      }
      if (prose) {
        const {left, width, top} = prose[0].getBoundingClientRect();
        column[0] = (left + width / 2) * ctx.cssPixelRatio;
        column[1] = width / 2 * ctx.cssPixelRatio;
        columnSpan[0] = ctx.toShaderY(prose[1].getBoundingClientRect().bottom);
        columnSpan[1] = ctx.toShaderY(top);
      }

      const previous = scroll;
      scroll = window.scrollY * ctx.cssPixelRatio * PARALLAX_SCROLLING_RATE;
      let step = rotate([0, (scroll - previous) * cameraZoom], cameraAngle);
      for (const offset of offsets) {
        offset[0] = (offset[0] + step[0]) % 1;
        offset[1] = (offset[1] + step[1]) % 1;
        step = rotate(step, layerTurn);
      }
    },
    shaders: {
      image: {
        uniforms: uniforms = {
          iResolution:          () => ctx.resolution,
          iWaveFrequency:       () => parameters.waveFrequency,
          iShapeSize:           () => parameters.shapeSize,
          iCameraAngle:         () => cameraAngle,
          iCameraZoom:          () => cameraZoom,
          iLensScale:           () => parameters.lensScale,
          iLensStrength:        () => parameters.lensStrength,
          iDrift:               () => drift,
          iOffset0:             () => offsets[0],
          iOffset1:             () => offsets[1],
          iOffset2:             () => offsets[2],
          iChromaticAberration: () => parameters.chromaticAberration,
          iPointer:             () => pointerTrail,
          iPointerRadius:       () => parameters.pointerRadius,
          iPointerRefraction:   () => parameters.pointerRefraction * presence,
          iPointerDispersion:   () => parameters.pointerDispersion * presence,
          iBlur:                () => blur,
          iBlurFalloff:         () => parameters.blurFalloff,
          iInitialLuma:         () => parameters.initialLuma * (1 + (parameters.skyLuma - 1) * daylight),
          iLumaFalloff:         () => parameters.lumaFalloff,
          iLayerZoom:           () => parameters.layerZoom,
          iLayerMagnification:  () => parameters.layerMagnification,
          iLayerEdge:           () => parameters.layerEdge,
          iLayerTurn:           () => layerTurn,
          iInnerGamma:          () => innerGamma,
          iOuterGamma:          () => outerGamma,
          iGradingRadius:       () => parameters.gradingRadius,
          iSkyRim:              () => parameters.skyRim,
          iSky:                 () => (themeSky ?? parameters.sky).map((channel) => channel * daylight),
          iColumn:              () => column,
          iColumnFeather:       () => column[1] * parameters.columnFeather,
          iColumnSpan:          () => columnSpan,
          iColumnFade:          () => column[1] * parameters.columnFade,
          iColumnVeil:          () => prose ? parameters.columnVeil : 0,
          iColumnColor:         () => columnColor,
        }
      }
    }
  }));

  fetch(SHADER_URL)
    .then((response) => {
      if (!response.ok) throw new Error(`${response.status} ${response.statusText}: ${SHADER_URL}`);
      return response.text();
    })
    .then(async (source) => {
      await domReady;
      insertShader(source);
      shade();
    })
    .catch((error) => console.warn("generative art deco background unavailable", error));

  window.generativeArtDeco = {
    parameters,
    // the uniform suppliers, once shader-web-background has asked for them
    get uniforms() { return uniforms; },
  };

})();
