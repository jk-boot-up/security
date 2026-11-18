# Detection as Code

```
src/main/java/com/jk/explore/security/detectioncode/
├── Attacks.java              Labelled attacks: for each rule 8 that the rule is written for and 2 evasions it is not (a different binary, a different spelling), and some attacks with no rule at all
├── Benign.java               Ordinary traffic of a bank for a week: logins, process starts on hosts, web requests
├── ConditionSteps.java       A teaching class: each selection of a rule decided on its own for one event, so the steps of a condition can be shown
├── Corpus.java               A week of recorded events from a bank's hosts and applications, made by a seeded generator, with the truth of every event
├── Coverage.java             The ATT&CK techniques the rules are tagged with against the techniques the labelled attacks used, and what each rule really caught
├── DetectionAsCodeDemo.java  Three process starts on a bank host: a web shell, the application server's health check, and a web shell under another server name
├── Engine.java               Runs a rule over events in time order and returns the ids of the events that raise an alert
├── Ev.java                   One recorded log event: a number, a time in milliseconds and flat text fields named as Sigma names them (Image, CommandLine, User)
├── Lookalikes.java           Benign events that look like attacks
├── Mutator.java              Small changes to a rule, each a loosening (more alerts) or a tightening (fewer): drop a filter, drop a field from a selection, drop one value of a list, change the first "and" to "or" or "1 of" to "all of", move a correlation threshold or window
├── OpenSearch.java           A single-node OpenSearch with the security plugin off, in a Docker container with a name of its own, on a free port; removed on close
├── Pysigma.java              pySigma with its OpenSearch backend, installed with pip into a virtual environment under build/ the first time, and the script that converts the rules
├── Rule.java                 A Sigma rule read from its YAML: a detection (selections and a condition) or an event_count correlation over another rule
├── RuleTests.java            The tests that sit beside a rule: rules/tests/NAME.json holds cases, each a few events and whether the rule must alert on them (should match) or must not (should not match)
├── Score.java                One rule against the truth
├── Sigma.java                The part of Sigma this project needs, evaluated in Java: selections of fields with the modifiers contains, startswith, endswith and all, wildcards, matching with no regard to case, a missing field that matches nothing, and the conditions and, or, not, parentheses, "1 of selection_ ", "all of them"
└── Truth.java                What an event really is
```

**8 Sigma rules run over a labelled week of 21,700 events catch 65 of the 82 attacks they are written for (the 17 evasions are the misses) with 57 false alarms; 3 of the 8 rules go over their false-alarm budget; 83 rule test cases kill 80 of 80 mutated rules; the ATT&CK matrix shows 4 of 20 techniques covered and 5 gaps; and the queries pySigma makes return the same events as the Java evaluation in OpenSearch, 8 of 8.**

A detection is code: it has an author, a test, a version and a cost in alerts. This project writes **8 Sigma rules** (YAML) for a bank's hosts and applications, each tagged with MITRE ATT&CK techniques: a shell started by a web server, a new administrator, credential dumping tools, a burst of failed logins, a download piped into a shell, cleared logs, a scheduled task and an injection in a web request. A seeded generator makes **a week of 21,700 recorded events** (benign traffic, look-alikes and 92 labelled attacks), a small Sigma evaluator in Java scores every rule against the truth, and **pySigma** converts the rules to queries that run in **OpenSearch** in Docker.

The demo prints six acts:
- **One rule, three events.** The rule's three selections are answered yes or no for each event, and the condition gives the alert: one attack caught, the health check rightly quiet, and an attack under another server name missed.
- **Scored.** 65 of the 82 attacks the rules are written for are caught; the 17 misses are spelled another way on purpose; 57 false alarms. Precision and recall are worked out for one rule.
- **Budgets and tuning.** Each rule may raise a number of false alarms a day; 3 of 8 go over. Dropping one filter takes 6 false alarms to 77; moving a threshold trades false alarms for misses.
- **Tests and mutants.** 83 should-match and should-not-match cases; 80 of 80 loosened or tightened rules fail them.
- **Coverage.** The ATT&CK techniques the rules are tagged with against those the attacks used: 4 of 20 covered, 5 gaps.
- **Conversion.** pySigma makes 8 queries; OpenSearch returns the same events as the Java evaluation for 8 of 8.

Acts one to five need only Java (`./gradlew test` checks them against the golden output); act six needs python3 and Docker (`./gradlew integrationTest`).

**Maps to:** MITRE ATT&CK T1505.003, T1136.001, T1003, T1110, T1105, T1070, T1053, T1190 · NIST CSF DE.CM, DE.AE · NIST SP 800-92.

## The idea in everyday terms

A smoke alarm with a test button and a log of every time it rang. You know how often it rings for toast, which fires it misses, and you can say so before the real fire.

## The scenario

A bank's security team keeps its detections in a repository. Every change is a rule file with tests, it is scored on recorded logs, and it must stay within a budget of false alarms a day.

## Run

```bash
./gradlew run
```

The demo tells the story in 6 acts, each printing exact numbers that the tests check:

| Act | What it shows |
| --- | --- |
| 1. One rule against three events | The rule web-server-shell has three selections: parent, child and a health-check filter. On three events it alerts on a web shell under nginx, stays quiet for the health check, and misses the same web shell under apache2. |
| 2. A week of logs, each rule scored | A seeded week of 21,700 events holds 92 labelled attacks. The 8 rules catch 65 of the 82 attacks they are written for, with 57 false alarms; for log-clearing, recall is 9 / 11 = 0.82 and precision 9 / (9 + 16) = 0.36. |
| 3. False-alarm budgets and tuning | Each rule may raise a number of false alarms a day; 3 of 8 go over. log-clearing raises 1 3 2 3 1 3 3 a day against a budget of 2: over on 4 of 7 days. Dropping web-server-shell's health-check filter takes 6 false alarms to 77; raising the failed-login threshold from 10 to 15 removes its 3 false alarms, and 20 misses 2 more attacks. |
| 4. Tests and mutants | web-server-shell has 9 test cases, 5 that must match and 4 that must not. Every rule's 83 cases pass. Each small change to a rule (a mutant) makes at least one case fail: 80 of 80 killed, 0 survived. |
| 5. ATT&CK coverage | The rules are tagged with 15 ATT&CK techniques and the labelled attacks used 19. Of 20 techniques in all, 4 are covered, 10 partial, 5 are gaps with no rule, and 1 is tagged but never attacked. |
| 6. pySigma and OpenSearch | pySigma 1.5.1 with the OpenSearch backend 2.0.3 converts 8 of 9 rules to Lucene queries (not the correlation rule). In OpenSearch 3.9.0, over the same 21,700 events, all 8 queries return exactly the events the Java evaluation alerted on, and the failed-login count over OpenSearch's results gives the same 11 alerts. |

## Test

```bash
./gradlew test
```

38 tests in `CorpusTest`, `CoverageTest`, `DemoRunsTest`, `EngineTest`, `OfflineOutputTest`, `RulesAsCodeTest`, `ScoreTest`, `SigmaTest`. Acts one to five (the Sigma evaluator, the correlation, the corpus, the scoring, the rule tests, the mutants and the coverage matrix) need no tools and are checked against the golden output by `./gradlew test`. Act six runs pySigma and OpenSearch in Docker (`./gradlew integrationTest`).

## What the simulation got right, and what it left out

Sigma, pySigma and OpenSearch are real; the logs are made by a seeded generator and are made up. The demo prints counts and rule names, never ports or container names.

## Technologies and versions

Every version below is pinned in the project's build files and was the latest stable release when the project was built.

| Technology | Version | Used for |
| --- | --- | --- |
| Java | 25 | the language and runtime; a Gradle toolchain finds or downloads the JDK |
| Gradle | 9.8.1 (wrapper) | build and run; nothing to install, the wrapper downloads it |
| tools.jackson.core:jackson-databind | 3.1.5 | library the code uses |
| tools.jackson.dataformat:jackson-dataformat-yaml | 3.1.5 | library the code uses |
| org.junit.jupiter:junit-jupiter | 6.1.3, set by junit-bom | JUnit 5: the tests (tests only) |
| org.junit.platform:junit-platform-launcher | 6.1.3, set by junit-bom | JUnit platform launcher, which Gradle needs to run the tests (tests only) |
| videokit | repository tool (`tools/videokit`) | the narrated video and animation: Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice) |

## Learning Material

| Document | What it is for |
| --- | --- |
| [Problem statement](docs/problem-statement.md) | the situation and what the project must show |
| [Prerequisites](docs/prerequisites.md) | what you need to know first |
| [Detection as Code, explained](docs/detection-as-code-explained.md) | the acts in prose |
| [Session guide](docs/session.md) | a one-hour lesson with exercises |
| [Animated walkthrough](docs/animation.html) | the acts step by step, narrated |
| [Threat model](docs/threat-model.md) | what is protected, who could harm it, and how, using STRIDE |
| [Spec](docs/spec.md) | what this project must be true of |
| [Expected output](docs/expected-output.txt) | the golden demo output every test and every port checks against |

### The idea in one picture

Rules are scored in Java and converted by pySigma.

![The parts](docs/images/architecture-diagram.png)

### Where each piece sits

The rule, the engine and the score.

![The classes and what they hold](docs/images/class-diagram.png)

### How the data moves

A rule becomes alerts that are counted against the truth.

![From a rule to a score](docs/images/data-flow-diagram.png)

### Who calls whom, in order



![A rule scored](docs/images/sequence-diagram.png)

### Video

`video/detection-as-code-explained.mp4` (with `.m4a` audio and `.srt` subtitles) is built by `video/build_video.sh`. Rendered media is not committed.

## What it costs

- **Evasions exist.** A rule written for one spelling misses another.
- **Filters are holes.** A filter that silences a false alarm is also a way past the rule.
- **Labelled logs are made by people** and fit the attacks they thought of.
- **Conversion is not complete.** Backends do not support every Sigma feature.
- **Budgets need upkeep** as the traffic changes; a threshold chosen on one week fits that week.

## When this is too much

A very small team with one log source can start with a few rules in the SIEM. Use detections as code when rules are shared, reviewed and changed often.

## Where you have already met this

- Rules edited in a SIEM console with no history.
- An alert that fires 200 times a day and is muted.
- A coverage slide that lists techniques with no test.
- A rule that nobody knows still works.

## Where this sits

This is a project of the [detection-engineering](..) module. It measures against a labelled truth, as [sast](../../devsecops-pipelines/sast) does, and the next project, [siem-basics](../siem-basics), collects and correlates the events such rules run on.
