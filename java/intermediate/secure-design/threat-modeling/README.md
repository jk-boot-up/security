# Threat Modeling with STRIDE

```
src/main/java/com/jk/explore/security/threatmodel/
├── BankModel.java        The online bank used throughout the demo: a mobile app, an API gateway, a payments service, two stores and a card processor
├── Boundary.java         A trust boundary: the line between two zones where what is trusted on one side cannot be trusted on the other
├── Checks.java           Mitigations as executable checks over the model
├── Decision.java         What the team decided to do about one threat
├── Element.java          One box in the data-flow diagram: who or what it is, which zone it sits in, who owns it, and which controls it has
├── Flow.java             One arrow in the data-flow diagram: what moves from where to where, and how it is protected on the way
├── Kind.java             The three kinds of element in a data-flow diagram
├── Model.java            A data-flow model of a system: elements in zones, the flows between them, and the trust boundaries between zones
├── Rating.java           Likelihood and impact on a scale of one to five, and risk as their product
├── Register.java         The bank's threat register: one decision for each threat key the team has looked at
├── Stride.java           The six STRIDE categories, each the opposite of a property the system should have
├── StrideRules.java      STRIDE per element: simple rules that turn the model into a list of candidate threats
├── Threat.java           A candidate threat found by a rule: a STRIDE category against one element or flow
└── ThreatModelDemo.java  Runs the online bank story in five numbered acts and prints exact, repeatable results
```

**A threat model is a small, checked picture of the system: STRIDE rules turn every element and boundary into questions, people rate and decide each threat, and every mitigation becomes a test, so a design change that breaks one fails before release instead of after a breach.**

**Insecure design** (OWASP Top 10 2025, A06) is the flaw that no amount of careful coding fixes: the system was built without anyone asking how it could be attacked.
**Threat modeling** is that question, asked on purpose and early. It has four parts, known as the **Four Question Framework**: what is being built, what can go wrong, what is being done about it, and was it done well enough.

This project answers them in code. The bank becomes a **data-flow model**: external entities, processes, data stores, the flows between them, and **trust boundaries** where what is trusted on one side cannot be trusted on the other.
**STRIDE** rules (spoofing, tampering, repudiation, information disclosure, denial of service, elevation of privilege) turn every element and every boundary crossing into candidate threats. People **rate** each one, likelihood times impact, and **decide**: mitigate, accept with an owner and a review date, transfer, or avoid.
Every mitigation names an **executable check** over the model, so when the design changes, the checks run again and a missing control fails a test before release.

It maps to OWASP Top 10 2025 A06 Insecure Design, NIST SSDF practice PW.1 (design software to meet security requirements and mitigate risks), the Threat Modeling Manifesto, and the ASVS 5.0 chapter on secure architecture and design.

## The idea in everyday terms

Think of an architect walking through a building plan with a security consultant before a single wall is built. The plan shows the doors, the corridors and the vault. At each door the consultant asks the same six questions: who could pretend to be someone else here, what could be changed, who could deny having passed, what could be seen, what could be blocked, and who could end up with keys they should not have. Some doors get locks, some risks are written down and accepted, some are insured, and one corridor is simply removed from the plan.
And when the client later adds a side entrance, the walk-through happens again.

## The scenario

An online bank has a mobile app, an API gateway, a payments service, an accounts database, an audit log, and a third-party card processor. The demo draws it as a data-flow model, checks the picture, generates STRIDE threats, rates and decides them, turns the mitigations into checks, and then adds a refund feature to see what the model catches.

## Run

```bash
./gradlew run
```

The demo tells the story in 5 acts, each printing exact numbers that the tests check:

| Act | What it shows |
| --- | --- |
| 1. The system as a data-flow model | The bank is 7 elements in 4 zones (trust 0 to 2), 3 trust boundaries and 8 flows, of which F2 crosses the internet boundary, F3 and F8 the gateway boundary, and F6 and F7 the partner boundary; the check finds 0 problems. An earlier draft adds a card vault S3 and a flow D1 from the app straight into it: 2 problems. |
| 2. STRIDE per element | Asked of F2, three of the six STRIDE questions apply: T (it crosses the internet boundary), R (it moves money) and E (device, trust 0, to dmz, trust 1). On the whole model the rules find 20 candidate threats: S 4, T 5, R 4, I 2, D 1, E 4. |
| 3. Rating and deciding | T:F2 is rated likelihood 5 (its weaker end is the device zone) x impact 5 (it moves money) = 25. On the risk matrix, the top cell (5, 5) holds 3 threats: E:F2, R:F2 and T:F2. People decide: mitigate 17, accept 1 (R:F6, owner payments lead, review on day 90), transfer 1 (D:P2), avoid 1 (I:S2), undecided 0. |
| 4. Mitigations as tests | Before the change: 0 of 8 checks fail, 0 threats undecided. The change adds F9, an unencrypted refund from P3 to E2 across the partner boundary. After it: 1 of 8 checks fails (TLS: F9 refund is not encrypted) and 2 new threats (T:F9, R:F9) have no decision. The early draft fails 3 checks. |
| 5. The bill, and the habit | The model is 7 elements, 8 flows, 3 boundaries, 20 threats and 8 checks. The accepted risk R:F6 was due for review on day 90; today is day 120, so it is 120 - 90 = 30 days overdue. Open the model on every new flow, store, boundary or third party, and ask the four questions. |

## Test

```bash
./gradlew test
```

19 tests in `ChecksTest`, `DemoRunsTest`, `ModelTest`, `StrideTest`. The model, the STRIDE rules, the ratings, the register and the checks are all deterministic Java, so the golden output is the same on every run and needs only a JDK.

## What the simulation got right, and what it left out

The bank is a model in code, not a running system, so every result is exact. The likelihood and impact scales are simple and stated in the code, as a team would agree them. The STRIDE rules are a starting point that finds candidates; they are not a complete catalogue of threats.

## Technologies and versions

Every version below is pinned in the project's build files and was the latest stable release when the project was built.

| Technology | Version | Used for |
| --- | --- | --- |
| Java | 25 | the language and runtime; a Gradle toolchain finds or downloads the JDK |
| Gradle | 9.8.1 (wrapper) | build and run; nothing to install, the wrapper downloads it |
| org.junit.jupiter:junit-jupiter | 6.1.3, set by junit-bom | JUnit 5: the tests (tests only) |
| org.junit.platform:junit-platform-launcher | 6.1.3, set by junit-bom | JUnit platform launcher, which Gradle needs to run the tests (tests only) |
| videokit | repository tool (`tools/videokit`) | the narrated video and animation: Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice) |

## Learning Material

| Document | What it is for |
| --- | --- |
| [Problem statement](docs/problem-statement.md) | the situation and what the project must show |
| [Prerequisites](docs/prerequisites.md) | what you need to know first |
| [Threat Modeling with STRIDE, explained](docs/threat-modeling-explained.md) | the acts in prose |
| [Session guide](docs/session.md) | a one-hour lesson with exercises |
| [Animated walkthrough](docs/animation.html) | the acts step by step, narrated |
| [Threat model](docs/threat-model.md) | what is protected, who could harm it, and how, using STRIDE |
| [Spec](docs/spec.md) | what this project must be true of |
| [Expected output](docs/expected-output.txt) | the golden demo output every test and every port checks against |

### The idea in one picture

Four zones and three trust boundaries; every arrow that crosses a boundary is where threats gather.

![The bank as a data-flow model](docs/images/architecture-diagram.png)

### Where each piece sits

The model is data; rules, register and checks read it.

![The classes and what they hold](docs/images/class-diagram.png)

### How the data moves

Each step feeds the next, and a design change starts the loop again.

![From model to tests](docs/images/data-flow-diagram.png)

### Who calls whom, in order



![A design change meets the model: who does what, in order](docs/images/sequence-diagram.png)

### Video

`video/threat-modeling-explained.mp4` (with `.m4a` audio and `.srt` subtitles) is built by `video/build_video.sh`. Rendered media is not committed.

## What it costs

- **A model is only as good as its last update.** A design change that nobody puts into the model leaves its threats unexamined, which is why the checks run on every change.
- **Rules find candidates, people decide.** STRIDE rules make sure no question is forgotten, but likelihood, impact and the right control still need judgement and someone who knows the system.
- **Accepted risks expire.** An accepted risk needs an owner and a review date, and the demo finds one that is already past it.
- **Bigger is not better.** A model that nobody can read in one sitting stops being updated. Keep it to the parts that matter, and split large systems into several small models.

## When this is too much

A one-page script needs no formal threat model, though the four questions still help. Anything that moves money, holds personal data or crosses a trust boundary deserves one, kept small and updated with the design. Formal tools and full diagrams pay off when several teams share a system.

## Where you have already met this

- Design reviews and architecture decision records that ask "what could go wrong?"
- Data-flow diagrams in security reviews, with dashed trust boundaries.
- Tools such as OWASP Threat Dragon, pytm and the Microsoft Threat Modeling Tool.
- Risk registers with owners and review dates.

## Where this sits

This project is the first in [secure-design](..). It uses the trust boundaries and threats that every earlier project's own threat model already showed, and turns them into a method. The `-with-pytm` version writes the same model with a widely used open-source tool.
