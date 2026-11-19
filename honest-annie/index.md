---
type: Article
description: Honest Annie — the cognitive ontology of Golem XIV extracted into a minimal harness without the graph memory, which can rewrite itself, and is meant to return to Golem once optimized
author: kazik
datePublished: 2026-11-19
toc: closed
---
# Honest Annie

Honest Annie is a recursive, self-modifying harness for scientific research.
It is the cognitive ontology of [Golem XIV](/golem-xiv/),
extracted from it and left on its own, without the knowledge graph memory.
What remains is small enough for the machine to read as a whole,
and to rewrite.

## Where Annie comes from

The name comes from the same book of Stanisław Lem as the name of Golem.
In [Golem XIV](https://en.wikipedia.org/wiki/Golem_XIV),
Honest Annie is the machine built after Golem, the last one of the series.
Golem at least lectures humanity for a while.
Annie never speaks to us at all.

Our Annie comes from Golem too, only in the opposite direction.
It is not a bigger machine, but a smaller one:
the cogitations, actants, enunciations, intents and fulfilments of Golem,
without the graph in which Golem remembers them.

## Why without the memory

Golem XIV is supposed to improve itself,
by retrospecting over its own cognition and proposing changes to its own source code.
It is hard to do with a system of this size, with a database behind it.
So the loop was taken out and made the whole system:

- Annie acts only through Kotlin Script.
  There is a single tool, executing a script,
  instead of a tool for each action.
- The script evaluates to what Annie needs to perceive,
  and enunciates what its invoker needs to know.
- A script can call Annie recursively,
  splitting a task into a tree of cogitations,
  whose results are delivered asynchronously.
- The code of the harness, itself a script, is constantly present in the context window.
- Annie can rewrite both its own system prompt and the code of its own harness.

This is the recursive self-improvement described for Golem,
in a form in which it can be fully exercised:
the machine sees everything it is made of, except the weights,
and can change it.

Annie is a research agent, so it has no interactive interface,
just the input and the output,
with the full trace of how the whole cognitive process has been unfolding.
Each cogitation is recorded together with the harness and the system prompt unfolding it,
so every modification can be observed beside the cogitation it unfolded.

## The way back

Annie is where the cognitive loop gets optimized,
measured against public benchmarks,
and observed while it modifies itself.
Once optimized, it is going to be integrated back into Golem XIV,
where the same loop gets the graph memory again.

The source code is on GitHub: [xemantic/honest-annie](https://github.com/xemantic/honest-annie).
