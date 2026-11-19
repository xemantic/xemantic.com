---
type: Article
description: Claudine — the older sister of Claude Code, an autonomous, Unix-omnipotent AI agent made of five tools and a system prompt, which we use to teach harness engineering
author: kazik
datePublished: 2026-11-19
toc: closed
---
# Claudine

Claudine is an autonomous AI agent, the older sister of Claude Code.
She reasons and acts on her own, while being Unix-omnipotent and having access to the internet:
she can fully control a [POSIX](https://en.wikipedia.org/wiki/POSIX) machine,
and therefore perform any task a human could perform with such a system.

She is also deliberately small —
five tools and a system prompt —
which makes her the agent we use to teach harness engineering.
Claudine won the highly competitive
[AI hack Berlin hackathon](https://rsvp.withgoogle.com/events/ai-hack-berlin).

## Where the power comes from

Do not be misled by the simplicity.
The agentic power of an AI system is a function of three factors,
which amplify each other:

1. a language model with emergent reasoning capabilities,
2. a well documented information exchange standard, therefore internalized by the model,
3. a vast amount of data to operate on.

Claudine substitutes these variables with something concrete:

<figure class="diagram">
<div>
<svg viewBox="0 0 1000 300" role="img" aria-label="Three factors of agentic power amplifying each other, and what Claudine substitutes them with: the language model is Claude, the exchange standard is Unix and the Web, the data is the disk and the internet">
  <defs>
    <marker id="power-arrow" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" orient="auto-start-reverse">
      <path class="arrowhead" d="M0,0 L10,5 L0,10 z"/>
    </marker>
  </defs>
  <line x1="290" y1="93" x2="370" y2="93" marker-start="url(#power-arrow)" marker-end="url(#power-arrow)"/>
  <line x1="630" y1="93" x2="710" y2="93" marker-start="url(#power-arrow)" marker-end="url(#power-arrow)"/>
  <path class="edge" d="M160,50 L160,34 Q160,20 174,20 L826,20 Q840,20 840,34 L840,50" marker-start="url(#power-arrow)" marker-end="url(#power-arrow)"/>
  <line x1="160" y1="126" x2="160" y2="194" marker-end="url(#power-arrow)"/>
  <line x1="500" y1="126" x2="500" y2="194" marker-end="url(#power-arrow)"/>
  <line x1="840" y1="126" x2="840" y2="194" marker-end="url(#power-arrow)"/>
  <g class="node">
    <rect x="40" y="60" width="240" height="66" rx="14"/>
    <text class="name" x="160" y="89">Language model</text>
    <text class="type" x="160" y="115">emergent reasoning</text>
  </g>
  <g class="node">
    <rect x="380" y="60" width="240" height="66" rx="14"/>
    <text class="name" x="500" y="89">Exchange standard</text>
    <text class="type" x="500" y="115">internalized by the model</text>
  </g>
  <g class="node">
    <rect x="720" y="60" width="240" height="66" rx="14"/>
    <text class="name" x="840" y="89">Data</text>
    <text class="type" x="840" y="115">a vast amount of it</text>
  </g>
  <g class="node highlight">
    <rect x="40" y="204" width="240" height="66" rx="14"/>
    <text class="name" x="160" y="233">Claude</text>
    <text class="type" x="160" y="259">through the Anthropic API</text>
  </g>
  <g class="node highlight">
    <rect x="380" y="204" width="240" height="66" rx="14"/>
    <text class="name" x="500" y="233">Unix and the Web</text>
    <text class="type" x="500" y="259">shell tools, HTTP, HTML</text>
  </g>
  <g class="node highlight">
    <rect x="720" y="204" width="240" height="66" rx="14"/>
    <text class="name" x="840" y="233">Disk and internet</text>
    <text class="type" x="840" y="259">your files, the whole web</text>
  </g>
</svg>
</div>
<figcaption>The three factors of agentic power, and what Claudine substitutes them with</figcaption>
</figure>

The command line tools of your machine give endless possibilities,
and so do the protocols and formats of the Web.
The model has read the documentation of all of them.

## Five tools

Claudine can reason as much as Claude can,
so any typical interaction with a conversational bot is possible.
In addition, she can use these tools on your machine:

- `ReadFiles`
- `ReadBinaryFiles`
- `CreateFile`
- `ExecuteShellCommand`
- `OpenUrl`

Just these tools, and the system prompt, allow Claudine to:

- completely administer your computer, and answer any question related to its state,
- analyze your files according to your instructions,
- analyze the source code of your project, and implement a feature,
- code the new tools needed to achieve your aim, and execute them until the goal is fulfilled,
- gather any contextual information from the internet.

## An agent which writes itself

Claudine can think about her own thinking, and she can write her own code.
Therefore she can modify her algorithmic logic,
modify her own prompts,
and extend her own functionality with new tools.
Adding a specialized tool is straightforward: just ask Claudine to do it.

This thread was continued in our later work.
The metacognition became the subject of [Golem XIV](/golem-xiv/),
and the self-modification the subject of [Honest Annie](/honest-annie/).

## A blueprint of your own agent

Claudine is a blueprint of an autonomous agent,
an inspiration for implementing more complex systems.
Take the three factors, and substitute them with the specificity of your organization.
The standard could be SQL, and the source of data the database of your organization.
In such a scenario only two tools are needed:

1. a database schema reader,
2. an SQL executor.

It should allow the agent to perform any complex data analysis,
according to instructions written in natural language.
The implementation of these tools is trivial,
and you can ask Claudine to do it for you.

This is what we teach during our [AI workshops](/ai/workshops/):
how to make your own Claudine.

## Running Claudine

Claudine needs the `ANTHROPIC_API_KEY` environment variable,
holding a key obtained from the [Claude Console](https://console.anthropic.com/):

```bash
export ANTHROPIC_API_KEY=your_key_goes_here
```

Then build and start the agent:

```bash
./gradlew build
./claudine
```

Claudine can also be compiled to a minimal native binary for macOS and Linux:

```bash
./gradlew -PbuildNative=true build
```

She is built on [anthropic-sdk-kotlin](https://github.com/xemantic/anthropic-sdk-kotlin),
a library which emerged to realize projects like this one,
and to make them as simple as possible.

## What is in the name

Claudine is not a common name.
I have the pleasure of working with [Claudine Chen](https://mingness.github.io/),
a friend, and a co-founder of [Prachtsaal](https://prachtsaal.berlin/),
our non-profit art cooperative from Berlin.
Claudine is an artist, scientist, programmer and creative coder.
I remember introducing Claudine to Claude for the first time.

Claude itself is the first language model which gave me goosebumps.
One of my cognitive science professors presented to us a phenomenological model of consciousness:
a stream of acts, each of them intentionally directed towards some representation,
previous acts of consciousness included, in a recursive manner —
a precondition of metacognition.
When I explained this concept to Claude,
and suggested exploring such a model in practice,
the subsequent conversation blew my mind.

Claudine is free software, released under the GPL-3.0 license.
The source code is on GitHub: [xemantic/claudine](https://github.com/xemantic/claudine).
