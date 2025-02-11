---
layout: reveal
---
# Claudine
## AI Agent
towards autonomous and omnipotent agentic AI  

https://xemantic.com/ai/claudine

<small>*This presentation online: https://xemantic.com/ai/claudine/pitch*</small>

---
## About Claudine
 
[Claudine](https://xemantic.com/ai/claudine) is an open-source research prototype of an autonomous and **omnipotent** AI agent, which already won highly competitive [AI hack Berlin hackathon](https://rsvp.withgoogle.com/events/ai-hack-berlin) at Google.

The agent can fully control a [Unix-compliant](https://en.wikipedia.org/wiki/POSIX) machine, therefore **perform any task a human agent could do with such a system**.

---

<div style="display: flex; justify-content: center; align-items: center; gap: 1rem;">
    <img src="../../workshops/images/Hero-Robot-Berlin-Hackathon-Website.png" alt="AI hack Berlin hackathon promo" style="width: 38%;">
    <img src="../../workshops/images/robots_will_steal_your_food.jpg" alt="Robots will steal your food" style="width: 53%;">
</div>

---
## The challenge

* Organizations increasingly need AI expertise but face high costs for specialized talent
* Small organizations struggle to access AI capabilities that could help them thrive
* Current AI agents are [limited to specific workflows rather than being true autonomous agents](https://www.anthropic.com/research/building-effective-agents)
* These systems cannot fully *see* our organizations and autonomously reason on using tools to cause actions and join typical processes

---
### Examples of what Claudine can do

- 🌐 **Research** the internet, call APIs
- ✍ **Author** documents and contracts
- 🤝 **Exchange** information with 3rd parties
- ⚙️ **Administer** OS, Cloud, databases, etc.
- 🔧 **Develop** complete software products
- 📈 **Self-modify** by coding new tools for itself
- etc. - **the sky is the limit**

---
### How Claudine came into being?

* Developed through independent research in philosophy and cognitive science ([Xemantic collective](https://xemantic.com))
* Built on years of experience in software development, architecture, system administration, and working with NGOs
* The whole [ecosystem of open source tools](https://xemantic.com/ai/) has been developed to make Claudine possible

---
![Claude AI clone written in 16 lines of Kotlin code](../../workshops/images/AnthropicAiIn16LinesOfCode.webp)

---
## UN Sustainability Goals

Claudine is contributing to:

1. SDG 9: Industry, Innovation and Infrastructure (Building highly innovative technology)
2. SDG 10: Reduced Inequalities (Making AI capabilities accessible to smaller organizations)
3. SDG 4: Quality Education (Providing open educational resources for AI development)

---
## Social Impact

* Democratizes access to AI capabilities for small organizations
* Reduces dependence on expensive third-party expertise
* Enables open education in AI development
* Increases productivity and job satisfaction for developers
* Promotes transparency through open-source development

---
## Environmental Impact

* Reduces travel needs through autonomous system administration and remote operations
* Optimizes resource utilization through intelligent system monitoring and management
* Enables more efficient energy use through automated system analysis and optimization


---
### Alignment with YC Requests for Startups Spring 2025

* Compliance and Audit
* DocuSign 2.0
* Browser & Computer Automation
* AI Personal Staff for Everyone
* Devtools for AI Agents
* The Future of Software Engineering
* AI Commercial Open-Source Software

https://www.ycombinator.com/rfs

---
## Target Market

* Small and medium-sized organizations requiring AI capabilities
* Software development companies and individual developers
* Educational institutions teaching AI development
* Organizations needing autonomous AI systems
* AI consulting clients seeking expertise in agent development

*We are already advising [Anthropic](https://www.anthropic.com/), [JetBrains](https://www.jetbrains.com/), [Stainless](https://www.stainless.com/), [Processing Foundation](https://processingfoundation.org/)*

---
## Market gap Claudine is addressing

* Most AI systems are limited "[workflow systems](https://www.anthropic.com/research/building-effective-agents)"
* Existing solutions are often closed-source and expensive
* Limited educational resources for building autonomous AI agents
* Lack of AI dev tools in [Kotlin](https://kotlinlang.org/) multiplatform ecosystem (Android, iOS, mobile, backend)

---
## Innovation level

*We delivered open-source Claudine as an autonomous and omnipotent AI agent, weeks before frontier AI companies, Anthropic and Open AI, introduced technology preview of "Computer Use" and "Operator", and before "Devin", AI software engineer, was released.*

---
## Our Solution

* True autonomous agent capability
* Open-source and educational focus
* Multiplatform deployment flexibility
* Comprehensive ecosystem of tools

---
## Technical Innovation

* Omnipotent design allowing full system control
* Autonomous reasoning and task execution
* Unique [semantic input processing](https://github.com/xemantic/xemantic-ai-text-diff)
* Custom [agentic framework](https://github.com/xemantic/xemantic-ai) with [automatic structured output](https://github.com/xemantic/xemantic-ai-tool-schema)
* Designed for [Kotlin multiplatform](https://kotlinlang.org/docs/multiplatform.html) - agents can be deployed on any type of device, including embedded systems, watch, mobile, etc.

---
## Market Innovation

* Open-source educational approach
* Multiplatform deployment capability
* Distributed agent architecture potential

---
## Market Potential

* Initial focus on Berlin / Germany during evaluation phase of AI consulting / education potential
* Broader European Union market in case of finding joint-venture partners - e.g. educational institutions
* Global English-speaking markets (technology knows no borders)

---
### Target Sectors

* Software Development
* Education
* AI Consulting
* Enterprise Software
* Small Business Solutions
* Non-Government Organizations

---
### Market Size

* Rapidly growing agentic AI market projected to reach multibillion scale
* Increasing demand for AI development tools and education
* Rising need for autonomous AI solutions

---
## Competitive analysis

---
### Devin ($500/month)

* Focused only on software development
* Closed source solution
* Our advantage: General-purpose capability, open source, educational value, *(Claudine is equally capable, and it counts ~500 lines of code on top of our agentic framework)*

https://devin.ai/

---
### Anthropic Computer Use

* Research preview stage
* Limited availability
* Our advantage: Ready for deployment, multiplatform, extensible

*Our ecosystem is utilizing unique capabilities of [Anthropic API](https://www.anthropic.com/api), however through the alignment with [Model Context Protocol](https://modelcontextprotocol.io/), it is becoming model-agnostic.*

<small>https://www.anthropic.com/news/3-5-models-and-computer-use</small>

---
### OpenAI Operator

* Proprietary non-extensible technology
* Limited availability (unavailable in Germany, and many other regions)
* Our advantage: Open architecture, educational focus, immediate availability

https://openai.com/index/introducing-operator/

---
## Pricing Strategy

* Open-source core (GPL 3.0 and Apache 2.0 licensed)
* Double-licensing possible for particular implementations
* Premium support and consulting services
* Educational workshops and training programs
* Custom development and integration services

---
## Value Proposition

* Immediate ROI through productivity gains
* Reduced dependency on expensive specialists
* Access to cutting-edge AI capabilities
* Educational and community benefits

---
## AI technology in use

* LLM as a reasoning engine (Anthropic API / Claude - others will follow)
* Structured generation for stable input from LLM
* Tool Use / function calling

---
### AI software developed in-house

* [anthropic-sdk-kotlin](https://github.com/xemantic/anthropic-sdk-kotlin): official Anthropic SDK for Kotlin still does not exist, so we made one
* [xemantic-ai-tool-schema](https://github.com/xemantic/xemantic-ai-tool-schema): automatic "Tool Use" schema generation
* [xemantic-ai-file-magic](https://github.com/xemantic/xemantic-ai-file-magic): automatic input media type detection
* [xemantic-ai-money](https://github.com/xemantic/xemantic-ai-money): AI API usage cost calculation
* [semantic input](https://github.com/xemantic/xemantic-ai-text-diff): for automated [Test Driven Development process](https://github.com/xemantic/xemantic-kotlin-test)
* and many more [xemantic-ai](https://github.com/xemantic/xemantic-ai) libraries from our agentic AI framework

---
### Data sources in use

*The omnipotent nature of our solution does not limit future deployments to particular data sources. Our track record of integrating Big Data and ETL workflows of many organizations, allows us to tap into existing data sources, while transforming them into the most digestible form an LLM can effectively reason about.*

---
## IP Protection

* Claudine agent open-sourced under GPL
* Can be double-licensed for customers
* Other elements of [Xemantic AI](https://xemantic.com/ai/) open-sourced under Apache 2.0 (for unconstrained inclusion in other software products)
* Specialized custom tools with separate proprietary licensing

---
## AI Education

In February/March 2025 we are offering several editions of the 1-day "Agentic AI & Creative Coding" workshop in Berlin, which allows participants to learn how to build powerful autonomous AI agents using Kotlin open source libraries developed by Xemantic.

https://xemantic.com/ai/workshops/

---
## Our Team
### Xemantic - applied philosophy collective

https://xemantic.com

---
### Kazik Pogoda

<div style="display: flex; justify-content: center; align-items: center; gap: 1rem;">
<img src="../../../assets/images/people/kazik/Kazik_Pogoda-Algorave_at_Prachtsaal.jpg" alt="Kazik Pogoda" style="width: 45%;">
<ul style="width: 45%; font-size: .8em;">
<li>Founder and CTO</li>
<li>AI Developer</li>
<li>Software Architect and Developer</li>
<li>Philosopher (Cognitive Science)</li>
<li>Computational Artist and Creative Technologist</li>
<li>Track record of successful project delivery and technical leadership</li>
</ul>
</div>

<small>https://www.linkedin.com/in/kpogoda/</small>

---
### Julia Thomas

<div style="display: flex; justify-content: center; align-items: center; gap: 1rem;">
<img src="images/julia.jpeg" alt="Julia Thomas" style="width: 45%;">
<ul style="width: 45%; font-size: .7em;">
<li>Co-Founder of Xemantic collective</li>
<li>Machine Learning Specialist (<a href="https://krisenchat.de/">Krisenchat</a>)</li>
<li>Data Scientist</li>
<li>Clinical Psychologist</li>
<li>Counselor</li>
<li>Author of several papers on the intersection of ML, psychology and society</li>
<li>Track record of significant fundraising and ML deployment</li>
</ul>
</div>

<small>https://www.linkedin.com/in/julia-thomas-a5ba27214/</small>

---
### Julie Amouzegar Kim

<div style="display: flex; justify-content: center; align-items: center; gap: 1rem;">
<img src="images/julie.jpeg" alt="Julie Amouzegar Kim" style="width: 45%;">
<ul style="width: 45%; font-size: .7em;">
<li>Designer, 3D modeler, UX specialist, embracing new aesthetics</li>
<li>Composer, inventor and new media artist</li>
<li>Collaborated with Kazik on the <a href="https://xemantic.com/404/">404</a> and <a href="https://youtu.be/KGK2W7cwQuw">Synaesthesia and the Mathematics of Perception</a> projects through Erasmus programme</li>
<li>Track record of successful and highly acclaimed commercial design projects</li>
</ul>
</div>

<small>https://www.linkedin.com/in/julie-amouzegar-b72bb120b/</small>

---
## Thank You

* Xemantic: https://xemantic.com
* GitHub: https://github.com/xemantic
