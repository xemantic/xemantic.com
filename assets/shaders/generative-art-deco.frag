/*
 * Generative art deco, the background of xemantic.com.
 *
 * A descendant of the "generative art deco" series on Shadertoy,
 * https://shadertoy.com/view/mdl3WX, see also The Mathematics of Perception,
 * https://www.shadertoy.com/view/7sVBzK, for the ideas behind it.
 *
 * The picture is a stack of layers, each one tiling the plane with the same
 * art deco cell. Every layer is seen through the one above it, magnified and
 * turned, so that the eye reads the stack as a space receding into depth.
 *
 * Every coefficient arrives as a uniform, and everything changing with time
 * is computed once per frame, instead of once per pixel,
 * see assets/js/generative-art-deco.js for the values.
 */
precision highp float;

uniform vec2  iResolution;

// the cell
uniform float iWaveFrequency;       // how many rings the wave of the cell makes
uniform float iShapeSize;           // where the wave of the cell is cut into a shape

// the camera, animated
uniform float iCameraAngle;
uniform float iCameraZoom;

// the lens
uniform float iLensScale;           // the reciprocal of the radius where the lens turns the view inside out
uniform float iLensStrength;

// the journey through the infinite plane, animated, shared by all the layers
uniform vec2  iDrift;
// the pan of each layer, following the scrolling of the page
uniform vec2  iOffset0;
uniform vec2  iOffset1;
uniform vec2  iOffset2;

uniform float iChromaticAberration;

// the pointer, changing the refraction of the glass under it
uniform vec2  iPointer;             // in screen space
uniform float iPointerRadius;       // how far its influence reaches
uniform float iPointerRefraction;   // the magnification added at the pointer, faded in and out
uniform float iPointerDispersion;   // the chromatic aberration added at the pointer, faded in and out

// depth of field and aerial perspective
uniform float iBlur;                // the blur of the top layer, animated
uniform float iBlurFalloff;         // how much of its blur every deeper layer keeps
uniform float iInitialLuma;         // the brightness of the top layer
uniform float iLumaFalloff;         // how much of it every deeper layer keeps

// descending into the next layer
uniform float iLayerZoom;           // the magnification of the next layer outside of the shape
uniform float iLayerMagnification;  // the extra magnification seen through the shape
uniform float iLayerEdge;           // the softness of the edge between the two
uniform float iLayerTurn;           // the angle every layer is turned by, animated

// color grading, the gammas animated
uniform vec3  iInnerGamma;
uniform vec3  iOuterGamma;
uniform float iGradingRadius;       // where the grading starts departing from the inner one towards the outer one

// the vignette
uniform float iVignetteInner;       // the radius where the darkening starts
uniform float iVignetteOuter;       // the radius where it goes black

const int LAYERS = 3;

// Rotates v counterclockwise by the angle a.
// assets/js/generative-art-deco.js mirrors it, keep the two in sync.
vec2 rotate(vec2 v, float a) {
    float c = cos(a);
    float s = sin(a);
    return vec2(c * v.x - s * v.y, s * v.x + c * v.y);
}

// The art deco cell, repeated over the whole plane: 1 on a dot at the center
// of every cell and in the gaps between neighbouring discs, 0 across the disc
// around the dot, with an edge as soft as the blur.
//
// Infinite space: fract() folds the whole plane into a single cell, so however
// far the coordinates travel - drifting, panning, zoomed in by every layer -
// there is always more of the pattern to show, at no cost.
float pattern(vec2 p, float blur) {
    vec2 cell = fract(p) * 2. - 1.; // from -1 to 1 across every cell
    float wave = sin(length(cell) * iWaveFrequency) * .5 + .5;
    return smoothstep(iShapeSize + blur, iShapeSize - blur, wave);
}

void main() {
    // centered at the middle of the screen, the shorter side spanning 1
    vec2 screen = (gl_FragCoord.xy - .5 * iResolution) / min(iResolution.x, iResolution.y);

    // Camera: the whole view sways back and forth, and slowly breathes
    // in and out.
    vec2 space = rotate(screen, iCameraAngle) * iCameraZoom;

    // The pointer thickens the glass under it: the closer, the stronger
    // every layer below refracts through the shapes of the one above it,
    // and the wider the colors of the light split apart.
    float touch = smoothstep(iPointerRadius, 0., length(screen - iPointer));
    float refraction = iLayerMagnification + touch * iPointerRefraction;
    float dispersion = iChromaticAberration + touch * iPointerDispersion;

    // Lens: every point is pushed away from the center by a factor growing
    // with the logarithm of its closeness to it. The pattern is compressed
    // ever more towards the middle of the screen, like the far end of a tunnel,
    // which gives the flat picture its depth. The logarithm is negative on
    // the whole screen, which also mirrors the view through the center.
    space *= log(length(space) * iLensScale) * iLensStrength;

    // Each pan already arrives rotated into the frame of the layer it belongs to,
    // so all of them shift in the same screen direction. It stays clear of the lens,
    // which keeps focused on the center, and of the per layer zoom below,
    // which is what leaves every layer travelling at its own pace - parallax.
    vec2 offsets[LAYERS];
    offsets[0] = iOffset0;
    offsets[1] = iOffset1;
    offsets[2] = iOffset2;

    // Depth of field: the top layer, the closest one, is drawn with the softest
    // edges, and every layer deeper is sharper, as if the camera were focused
    // far away. The blur of the top layer pulses, the focus hunting for a subject.
    float blur = iBlur;
    // aerial perspective: the deeper the layer, the darker it gets
    float luma = iInitialLuma;

    vec3 color = vec3(0.);
    for (int i = 0; i < LAYERS; i++) {
        vec2 p = space + offsets[i] + iDrift;

        // Chromatic aberration: like a real lens, which refracts every wavelength
        // a little differently, the red and blue channels are sampled slightly
        // off, towards and away from the center, with the fringes widening
        // towards the edges of the screen.
        vec2 aberration = space * dispersion;
        color += vec3(
            pattern(p - aberration, blur),
            pattern(p, blur),
            pattern(p + aberration, blur)
        ) * luma;
        color = min(color, 1.);

        // Descending into the next layer, seen through this one: where this layer
        // shows its shape, the next one is magnified much more than elsewhere
        // (the sharp edge keeps the two regions crisp), like looking through a
        // window of thick glass. The whole layer is also turned by an angle
        // swinging with time, and its pattern, zoomed in, reads as farther away.
        float zoom = iLayerZoom + pattern(p, iLayerEdge) * refraction;
        space = rotate(space * zoom, iLayerTurn);

        luma *= iLumaFalloff;
        blur *= iBlurFalloff;
    }

    // Color grading: every channel gets its own gamma, drifting with time.
    // Two gradings, detuned in frequency, are blended by the distance from
    // the center, so the tint of the middle of the screen wanders apart
    // from the one of its edges.
    float radius = length(screen);
    vec3 gamma = mix(iInnerGamma, iOuterGamma, radius - iGradingRadius);

    // the vignette, darkening the corners
    float vignette = smoothstep(iVignetteOuter, iVignetteInner, radius);

    vec3 graded = pow(color, gamma) * vignette;
    // squared for deeper blacks, keeping the background calm under the text
    gl_FragColor = vec4(graded * graded, 1.);
}
