---
layout: reveal
---
## Golem XIV
### towards metacognitive AI

![this presentation - QR code](https://quickchart.io/qr?text=https://xemantic.com/ai/workshops/dvlp.energy/)

<small><https://xemantic.com/ai/workshops/dvlp.energy/></small>

---
## Agenda

- about Xemantic and me
- about metacognition and Golem XIV
- live demo
- questions

---
## What will you learn

- cognitive science
- agentic AI
- context engineering
- (meta)cognitive AI

---
## About Xemantic

- 

Prachtsaal is a non-profit art cooperative, established by resident artists of the space together with the owners of this building. We are democratically managing Prachtsaal as an experimental and diverse cultural center, while inviting our community in.

https://prachtsaal.berlin

---
### Robots will steal your food

<div style="display: flex; justify-content: center; align-items: center; gap: 1rem;">
    <img src="../../../ai/workshops/images/robots_will_steal_your_food.jpg" alt="Robots will steal your food" style="width: 53%;">
</div>

https://github.com/xemantic/claudine/

Since Claudine (like Claude Code) I am writing only software which machines still cannot write.

## Xemantic's open source software

---
## About Golem XIV


---
## What is "cognition"

Cognition defines reasoning process in abstraction from the biological substrate

- carbon (biological)
- silicon (tech)
- alien (sci-fi)

Cognitive science is focused on defining models of mind.

---
## Is LLM a cognitive machine?

Very controversial topic:

Apple: [The Illusion of Thinking](https://machinelearning.apple.com/research/illusion-of-thinking)

OpenAI

---
## About metacognition

Metacognition: the capability of "thinking about own thinking".

Attributed to humans and many animal species. In popular culture called self-consciousness. 

Xemantic is a collective of applied philosophy based at Prachtsaal, founded by Julia Thomas and Kazik Pogoda, conducting independent AI research and creating immersive computational art.

We publish our research as open source software.

https://xemantic.com

https://github.com/xemantic

---
### Xemantic's shows

<iframe width="560" height="315" src="https://www.youtube.com/embed/Hb-P2f0cyMI?si=uDb8Uo-zzsxmzXtT" title="YouTube video player" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" referrerpolicy="strict-origin-when-cross-origin" allowfullscreen></iframe>

https://www.instagram.com/xemantic.berlin

---
### Xemantic @ AI hack Berlin

<div style="display: flex; justify-content: center; align-items: center; gap: 1rem;">
    <img src="../images/Hero-Robot-Berlin-Hackathon-Website.png" alt="AI hack Berlin hackathon promo" style="width: 38%;">
</div>


---
## Introductions

Tell us a bit about yourself and your AI experience.

- Generative AI
- Vision models
- AI assisted development
- Retrieval Augmented Generation (RAG)
- AI agents

---
### Which AI software are you using?

- ChatGPT (etc.)
- Cursor (AI-assisted development)
- n8n (AI workflow automation)
- Langchain (agentic AI frameworks)
- Frontier AI Lab APIs
- Claude Code (Codex, Gemini CLI ...)

---
## dvlp.energy wishlist

- authoring emails using private knowledge
- chatbot understanding dvlp.energy
- prerequirement: semantic knowledge extraction
- data sources: website, blog, loom videos
- n8n: setting up agentic workflows (is it still relevant with autonomous agents)

---
## Software to install

1. IntelliJ: https://www.jetbrains.com/idea/
2. Optionally GitHub Desktop if it is your first time with git: https://github.com/apps/desktop
3. Clone workshop GitHub repository: https://github.com/xemantic/xemantic-ai-workshop
4. Clone Claudine GitHub repository: https://github.com/xemantic/claudine

---
## Ask me questions, anytime!

If something is unclear, or I am using a new term which was not explained, or you need to correct me, please let me know immediately. AI field is changing so fast, that we are learning each and every hour.

---
## Agentic AI

What makes an AI agent?

- workflow systems
- agent with autonomy

<small>https://www.anthropic.com/research/building-effective-agents</small>

---
## Why Anthropic?

*instead of OpenAI, DeepSeek, Mistral ...*

* structured generation/output (not so good in open source models)
* reduced *context rot* for long contexts
* long-run reasoning

_Claude Sonnet 3.5 - the first model which made truly autonomous agentic use cases feasible (SWE-Bench)_
_Xemantic invested in building anthropic-sdk-kotlin the base for Claudine agent_

---
## Claudine
### Live session

![claudine on GitHub - QR code](https://quickchart.io/qr?text=https://github.com/xemantic/claudine)

https://github.com/xemantic/claudine/

---
## anthropic-sdk-kotlin

- Kotlin-idiomatic - minimal syntax, maximal meaning
- own agentic framework
- multiplatform
- base for Claudine

<small>https://github.com/xemantic/anthropic-sdk-kotlin</small>

---
### Workshop repository

Examples for cross-reference:
- Kotlin (minimal syntax for illustrating concepts)
- Python
- TypeScript

---
## What you will learn?

- **prompt engineering**: English as a programming language
- **context engineering**: managing meanings in time to kick off AI cognition
- **cognitive Science**: the psychological and philosophical foundation of a technique

---
## A glossary of AI-related terms

Navigating through Agentic AI development requires particular vocabulary:

### `ai_glossary.md`

---
## Time for demonstration

### `Demo01HelloWorld.kt`

---
## Looking at Claudine again

Now all the patterns should be clear

---
# Back to meta ...
## Why is it even possible?

---
## Emergence

Any evolution of self-replicating systems, when scaled, exhibit holistic properties non-reducible to properties of it's building blocks.

_Biology as a metatheory of computer science? ;)_

---
## New phenomena in Machine Learning

_from ML to AI_

- scaling laws (just throw more compute at it...)
- emergent reasoning
- emergent theory of mind

---
## The tectonic paradigm shift

... in virtually everything, but it starts with software

---
### Are robots stealing our jobs?

We need to adapt extremely fast.

https://openai.com/index/gdpval/

---
### Products of AI era

- is UI/UX design going to survive?
- what kind of skills are needed from software developers?
- what kind of skills are needed from knowledge workers?

*In the end-to-end process, we are at both ends - specs and QA, machines take the middle*

---
### What does Agentic AI imply for software development?

**Stop the world! ... and rethink.**

We are at the tipping point. Every project we are working on at the moment might be obsolete when we release it.

---
## Rethink tools and approaches

- probabilistic vs algorithmic solution
- "AI-assisted development" vs "assisting AI in development"?
- Polish as the most powerful programming language (Natural languages)
- is "spec" a new "software"?
- TDD more relevant than ever
- How do we store knowledge?

---
## Retrieval Augmented Generation (RAG)

- hard to set it right (chunking).
- in agentic AI era RAGs seem to be less efficient.

---
## AX - AI/Agent experience

Code is to be regenerated each time with LLM. Specs are what matters:

Test Driven Development with AI:

https://github.com/xemantic/xemantic-kotlin-test

---
## Empathy Driven Development

How to design AX-friendly APIs?
Goodbye REST and JSON - Welcome, Wikipedia.

Case study:

https://markdown.law

---
## Model Context Protocol

USB for LLMs - protocol over our tool use:

https://modelcontextprotocol.io/docs/getting-started/intro

---
## Skills

Tools and MCP exhibit serious limits with agents - new approach, direct code execution and skills:

https://claude.com/blog/skills

---
## Specs and evals

*How to tell if AI is delivering in our business process?*

Define evals - like integration tests, catch reasoning regressions.

---
## Reducing cognitive load for AI

- You are not an HTML parser, LLM is not a JSON parser
- less is more - avoid double task inference
- agent work in a feedback loop - make sure they comprehend your errors

---
### Agentic AI principles

1. A language model with emergent reasoning capabilities.
2. Well documented (therefore internalized by the model) information exchange standard.
3. Vast amount of data to operate on.

What can you substitute for these categories at dvlp.energy?

---
## AI Alignment, privacy and security

*Ethics is the most profitable product of the AI revolution*

---
## AI 2027

> "We predict that the impact of superhuman AI over the next decade will be enormous, exceeding that of the Industrial Revolution."

https://ai-2027.com/

---
## The Intelligence Curse

https://intelligence-curse.ai/

---
## Predicting the future

Dario Amodei, Anthropic's CEO, wrote [Machines of Loving Grace](https://darioamodei.com/machines-of-loving-grace#5-work-and-meaning) - an extremely insightful essay on machine intelligence, with predictions for the upcoming decade.

---
## Thank You!
### Stay in touch

![Xemantic discord server](https://quickchart.io/qr?text=https://discord.gg/vQktqqN2Vn)

[Xemantic discord server - follow this link](https://discord.gg/vQktqqN2Vn)
