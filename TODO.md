# TODO

Open items waiting for a decision.
The previous list is kept in [TODO-old.md](TODO-old.md).

## Hosting and analytics

- [ ] Move the website from GitHub Pages to Cloudflare, and make the repository private.
  GitHub Pages serves a private repository only on a paid plan,
  while Cloudflare (Pages, or Workers static assets) builds from a private repository on the free plan.
- [ ] Enable Cloudflare Web Analytics once the site is on Cloudflare.
  The legacy Jekyll layout loaded its beacon script,
  the new page layout does not, so no analytics are collected now.
  Analytics enabled on the Cloudflare side are injected automatically,
  with no script to add to the pages.

## Design

- [ ] Choose the `theme-color` of the site, coloring the browser UI on mobile.
  The shader background stays dark in the light color scheme too,
  so it is not obvious whether it should follow the color scheme,
  or always be dark.
- [ ] Should the first paint wait for the shader background?
  Now `shader-web-background.js` and `generative-art-deco.js` are deferred,
  and the background appears once the shader is fetched.
  Blocking would delay every first visit by three requests.

## Content

- [ ] Write the content of the placeholder pages:
  [Products](products/index.md), [Research](research/index.md), [Art](art/index.md) and [News](news/index.md).
- [ ] Describe the workshops in more detail in the structured data:
  the `Course` of `ai/workshops/index.md` could get a `hasCourseInstance` for each upcoming session
  (dates, location, mode) and `offers` (the price),
  which search engines need to show a course rich result.
- [ ] Confirm Buttondown is still the newsletter provider:
  the subscription form, lost with the legacy Jekyll layout,
  is back in the footer of every page, posting to `buttondown.com/xemantic`.

## Licensing

- [ ] Annotate the third-party images in `REUSE.toml`:
  `ai/workshops/images/Hero-Robot-Berlin-Hackathon-Website.webp` is the AI Hack Berlin promo artwork of Google,
  currently falling under the site-wide CC BY 4.0, which is not ours to grant.
  The photograph `robots_will_steal_your_food.webp` may also need its author credited.

## markanywhere

- [ ] Fix the parsing of an image nested in a link, `[![alt](image.webp)](https://…)`,
  which renders as literal text.
  `ai/workshops/index.md` works around it with raw HTML for the Prachtsaal banner.
