---
type: Article
description: Unix pipes for meaning — Markdown, HTML and YAML flowing as one stream of semantic events, built up example by example
author: kazik
datePublished: 2026-11-15
toc: closed
---
# markanywhere

## It starts with a pipe

```bash
cat diary.txt | grep rain | wc -l
```

Three small programs, and one question answered:
on how many days did it rain?
The first one reads, the second one filters, the third one counts.
None of them knows anything about the other two,
and none of them waits for the whole diary before it starts working —
the lines just flow through the pipe.

This idea is more than fifty years old, and it has not aged at all.
Small blocks, one shared shape of data between them,
and a way of composing them which fits in a single character.

## What if we had the same for Markdown?

We can try with what we already have:

```bash
cat README.md | grep '^# '
```

It finds the title of the document.
It also finds every comment in every shell example of that document,
because a pipe of characters cannot tell a heading from a line of code.
What flows through a Unix pipe is text, and what we care about is meaning.

So let us keep the pipe, and change what flows through it:

```kotlin
val markdown = """
# Hello

A *streaming* parser, <b>live</b>.
"""

flowOf(markdown)
    .parse()
    .collect { println(it) }
```

```json
{"type":"mark","name":"h1"}
{"type":"text","text":"Hello"}
{"type":"unmark","name":"h1"}
{"type":"mark","name":"p"}
{"type":"text","text":"A"}
{"type":"text","text":" "}
{"type":"mark","name":"em"}
{"type":"text","text":"streaming"}
{"type":"unmark","name":"em"}
{"type":"text","text":" parser,"}
{"type":"text","text":" "}
{"type":"mark","name":"b","tagged":true}
{"type":"text","text":"live"}
{"type":"unmark","name":"b","tagged":true}
{"type":"text","text":"."}
{"type":"unmark","name":"p"}
```

This is **markanywhere** ([source](https://github.com/xemantic/markanywhere)),
a Kotlin multiplatform library,
and this is everything which ever flows through its pipes —
three kinds of events:

- `text` — a chunk of characters,
- `mark` — a meaning opens: a heading, an emphasis, a block of code,
- `unmark` — the meaning closes.

Like `grep`, the parser does not wait for the end of its input.
The `h1` opens before `Hello` arrives,
and the `em` opens the moment the `*` resolves.
The Markdown `*streaming*` and the literal `<b>` tag become events of the same kind,
the latter only remembers, as `tagged`, that it was written as a tag.

## What if the output was HTML?

The events have to end up somewhere.
The last block of the pipe turns them back into characters:

```kotlin
flowOf(markdown)
    .parse()
    .renderHtml()
```

```markup
<h1>
  Hello
</h1>
<p>
  A <em>streaming</em> parser, <b>live</b>.
</p>
```

Two blocks, and we have a Markdown to HTML converter.
Not much of an achievement, there are plenty of them.
It only gets interesting when something is put between the two.

## What if the HTML could be enhanced?

Between parsing and rendering there is a stream of meanings,
and a meaning can be matched and rewritten while it flows:

```kotlin
flowOf(markdown)
    .parse()
    .transform {
        match("h1") { "h2" { children() } }
        match("em") { "strong" { children() } }
        passthrough()
    }
    .renderHtml()
```

```markup
<h2>
  Hello
</h2>
<p>
  A <strong>streaming</strong> parser, <b>live</b>.
</p>
```

Each `match` is a template, like in XSLT:
it selects a mark, by its name or by a predicate over its attributes,
and says what to emit in its place,
while `children()` carries on with whatever the mark holds.
Everything else passes through untouched.

A set of such rules is an ordinary function, so it becomes a block of its own,
with a name, to be put in any pipe:
one wrapping a card in a link,
one turning a list of links into a row of icons,
one adding a table of contents.

## What if it was a whole page?

A page is more than its content, it has a title, an author, a language.
In Markdown they live in the YAML front matter,
and markanywhere does not treat it as an opaque string —
the front matter flows through the pipe as events too:

```kotlin
val document = """
---
title: Hello
author: Alice
---
# Heading
"""

flowOf(document)
    .parse()
    .wrapInHtmlDocument()
    .renderHtml()
```

```markup
<html>
  <head>
    <title>Hello</title>
    <meta name="author" content="Alice"/>
  </head>
  <body>
    <h1>
      Heading
    </h1>
  </body>
</html>
```

## What if the pipe was long?

The page you are reading went through one.
This is, slightly abridged, the generator of this website
([source](https://github.com/xemantic/xemantic.com)):

```kotlin
markdown
    .parse()
    .ensureFrontmatterTitle()
    .wrapInSections(tocDepth = 6)
    .wrapInHtmlDocument()
    .layOutArticle(page)
    .addPageMetadata(page)
    .completeImages(page)
    .applyContentWidgets()
    .applyPageLayout(page, footer)
    .asHtml()
```

A missing title is derived from the first heading,
the headings are nested into sections with a table of contents,
the front matter becomes the `head`,
the text is laid out for reading,
the images learn their dimensions,
the navigation and the footer are woven around the content.
Every line is one block, knowing nothing about the others,
and the Markdown source stays as plain as it was.

## What if the pipe ran backwards?

A stream of events does not have to come from Markdown.
It can be a web page as well, with all its presentational wrappers,
icon fonts and tracking scripts:

```kotlin
val page = semanticEvents(tagged = true) {
    "body" {
        "h1" { +"Weather" }
        "p" {
            "i"("class" to "fa-solid fa-sun") { }
            +" Sunny and "
            "strong" { +"warm" }
            +" today — see the "
            "a"("href" to "https://example.com/forecast") { +"forecast" }
            +"."
        }
        "script" { +"track('view')" }
    }
}

page
    .transformHtmlToMarkdown()
    .renderMarkdown()
```

```markdown
# Weather

☀️ Sunny and **warm** today — see the [forecast](https://example.com/forecast).
```

The icon became an emoji, the script disappeared:
the meaning survives, the noise does not.
This is the Markdown a language model reads with ease,
and what [Umwelt](https://github.com/xemantic/umwelt) is built on —
a real browser at one end of the pipe, an AI agent at the other.
[markdown.law](https://markdown.law/) runs the same way,
with legislation published as XML and HTML coming in,
and Markdown going out.

## What if the stream never stood still?

The diary from the first example was a file, complete before we started.
A language model does not write files, it speaks,
token by token, and we read while it speaks:

```kotlin
document.body!!.appendSemanticEvents(
    tokens.parse()
)
```

Here the pipe ends not in a string, but in the DOM of a web page.
A heading appears before its text arrives,
and nothing is ever parsed, rendered or laid out again when the next token lands.
This is how [Golem XIV](https://github.com/xemantic/golem-xiv) shows its reasoning.

It is also why markanywhere exists.
We used to preserve text as documents — a scroll, a codex, a book —
and the software we built assumes a document is complete before it is processed.
But the dialogue between humans and machines is not a document,
it is a stream, comprehended while being generated.

## Anything else?

The blocks are few, the pipes are many.
The same stream of events can redact the sensitive spans of a text,
route a tagged block of a model's response to a separate sink,
extract structured data from a conversation,
or describe the expected output of a test, so that the test reads like what it asserts.
Wherever meaning flows as text, markanywhere can be there.

## Build your own pipe

markanywhere is published to [Maven Central](https://central.sonatype.com/namespace/com.xemantic.markanywhere).
In `build.gradle.kts` add:

```kotlin
dependencies {
    implementation("com.xemantic.markanywhere:markanywhere-parse:0.6.2")
    implementation("com.xemantic.markanywhere:markanywhere-render:0.6.2")
}
```

The [README](https://github.com/xemantic/markanywhere#readme) documents every module in detail,
and each example there is backed by a test.
