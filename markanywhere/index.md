---
description: A Swiss army knife for meanings — streaming Markdown, HTML and YAML as one flow of semantic events
---
# markanywhere

**markanywhere** ([source](https://github.com/xemantic/markanywhere)) is a Kotlin multiplatform library
which treats text not as a document, but as a stream of meaning.
It parses Markdown incrementally,
turning it into a flow of semantic events,
and gives you the tools to transform this flow into whatever the receiving context requires:
HTML, a live browser DOM, Markdown again, or something else entirely.

Think of it as a Swiss army knife for meanings:
one small, sharp abstraction,
and many blades built on top of it.

## Why a stream, and not a document?

We use language to convey meaning,
and we use text to express language.
For centuries text was preserved as a document —
a scroll, a codex, a book —
and the software we built inherited this paradigm,
assuming that a document is a complete artifact to be parsed, transformed and rendered.

But the way we communicate has changed.
Messages, chats, and above all the dialogue between humans and large language models
are not documents to be opened and read,
they are unfolding streams, comprehended while being generated.

markanywhere inverts the traditional flow of document processing.
Instead of consuming complete documents and producing a tree,
it consumes streaming tokens and emits semantic events in real time,
each one committed as soon as the parser knows what it means.

## The ontology of a meaningful stream

Everything markanywhere does is expressed with just three kinds of events:

- `Text` — a chunk of characters,
- `Mark` — the opening of a modality of expression, like a heading, an emphasis, or a block of code,
- `Unmark` — its closing.

Each mark remembers its origin:
whether it was derived from Markdown syntax,
or written as a literal HTML or XML tag.
Thanks to this, the very same stream can carry pure Markdown,
pure HTML, or HTML embedded in Markdown,
and the distinction survives end-to-end.
Even the YAML front matter is not an opaque string,
but events which can be queried and transformed with the same machinery as the rest of the text.

## The blades

- **Parsing** —
  an append-only, streaming parser of GitHub Flavored Markdown,
  the dialect language models were trained on,
  extended with highlights, math, YAML front matter and namespaced custom tags.
- **Rendering** —
  to an HTML string, back to Markdown,
  or, in Kotlin/JS, straight into the DOM of a web page, token by token.
- **Transforming** —
  a DSL resembling XSLT templates,
  matching marks and rewriting them on the fly,
  while the stream is still flowing.
- **HTML to Markdown** —
  the same pipeline running in reverse:
  a captured web page, with all its presentational wrappers, icon fonts and tracking scripts,
  distilled into clean Markdown, ready to be handed to a language model.
- **Browsing** —
  capturing a live page in a real Chrome,
  with every actionable element stamped with a reference,
  so that an AI agent can read the Markdown, and then click a link back on the live page.
- **Testing** —
  a builder describing the expected stream of events,
  so that tests read like the output they assert.

## Use cases

### Receiving LLM inference in Golem XIV

[Golem XIV](https://github.com/xemantic/golem-xiv) is our metacognitive AI system,
with knowledge-graph memory and self-directed research.
The tokens of its inference arrive as a stream,
and markanywhere renders them directly into the DOM of its user interface as they come:
a heading opens before its text arrives,
an emphasis opens the moment the `*` resolves,
and nothing has to be re-parsed, re-rendered or re-flowed when the next token lands.

### The generator of this very website

The page you are reading was written in Markdown
and turned into HTML by markanywhere
([source](https://github.com/xemantic/xemantic.com)).
The generator is a single pipeline of stream operators:
the Markdown is parsed,
a missing title is derived from the first heading,
headings are nested into sections with a table of contents,
the front matter becomes the `<head>` of the HTML document,
the page layout is woven in with the transformation DSL,
and finally everything is rendered as HTML.

### The markdown.law transformation

[markdown.law](https://markdown.law/) makes legislation available in Markdown,
a format equally comfortable for human readers and AI agents.
Legal texts are published in HTML and XML formats of their own,
and markanywhere transforms these sources, as streams of semantic events,
into clean, well-structured Markdown.

### And more

The same stream of events can redact sensitive spans of a text,
route a tagged block of an LLM response to a separate sink,
extract structured data from a conversation,
or turn a web page into Markdown an AI agent can act upon —
which is exactly what [Umwelt](https://github.com/xemantic/umwelt) does.
Wherever meaning flows as text, markanywhere can be there.

## Getting started

markanywhere is published to [Maven Central](https://central.sonatype.com/namespace/com.xemantic.markanywhere).
In `build.gradle.kts` add:

```kotlin
dependencies {
    implementation("com.xemantic.markanywhere:markanywhere-parse:0.4.0")
    implementation("com.xemantic.markanywhere:markanywhere-render:0.4.0")
}
```

Then parse a stream of Markdown, and render it as a stream of HTML:

```kotlin
val html: Flow<String> = markdownFlow
    .parse()
    .asHtml()
```

The [README](https://github.com/xemantic/markanywhere#readme) documents every module in detail,
and each example there is backed by a test.
