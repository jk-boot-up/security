# Kubernetes Network Policies

```
src/main/java/com/jk/explore/security/k8snetpol/
├── Cluster.java                        A real Kubernetes cluster made by kind (one node, itself a container), with its own kubeconfig file, deleted on close
├── Intent.java                         Which pod may reach which pod, as the design says, and a measured matrix compared with it
├── KubernetesNetworkPoliciesDemo.java  
├── Lab.java                            The bank's five pods on a real cluster, a server outside it, and the measurement of who can reach whom
├── Policies.java                       The bank's network policies as manifests, one small builder for each shape
├── Sh.java                             Runs a program and returns what it printed, whether it worked or not
└── Workload.java                       A pod of the bank: where it runs, its name, and the label that policies select it by
```

**Five pods of a bank on a real cluster (kind) reach each other on 20 of 20 pairs with no policy, 17 of them unwanted; a default deny closes all 20 and one allow for each line of the design opens exactly the 3 that are meant, and the measurement equals the design; three mistakes that the API server accepts show up only in the measurement: an allow for a whole namespace (1 unwanted pair), a typo in the app label of an allow (1 missing pair) and a deny written for one app that forgets a pod (4 unwanted pairs); a default deny of egress also stops the name lookup until DNS is allowed, and stops a server outside the cluster from being called; and a pod relabelled app=web gets the web's access.**

A Kubernetes cluster is a flat network: every pod can reach every other pod unless a **NetworkPolicy** says otherwise. This project runs a real cluster with **kind** (its default network plugin, kindnet, enforces policies), puts five pods of a bank in three namespaces and **measures** which pod can reach which. A Java program applies the policies and runs `curl` inside each pod against every other pod (`brew install kind kubectl`, Docker running); the demo makes a cluster, so it is tagged `integration`: `./gradlew integrationTest`.

What it shows:
- **A flat network.** With no policy, **20 of 20** pairs of pods reach each other; the design allows 3, so **17 are unwanted**.
- **Default deny, then the design.** One deny closes all 20 pairs; one allow for each line of the design opens exactly **3 of 20** and the measurement equals the design.
- **Three mistakes the API server accepts.** An allow for a whole namespace (**1 unwanted** pair), a typo in an app label (**1 missing** pair), a deny written for one app that forgets a pod (**4 unwanted** pairs). Only the measurement shows them.
- **Egress and DNS.** Denying egress also stops the name lookup until DNS is allowed, and stops a taken-over api from calling a server outside the cluster.
- **The label is the credential.** A pod relabelled `app=web` gets the web's access.

**Maps to:** NIST CSF 2.0 Protect (PR.IR) · NIST SP 800-53 SC-7 · CIS Kubernetes Benchmark 5.3 · Zero trust microsegmentation.

## The idea in everyday terms

An office building where every door is unlocked. Locking them all is the easy part. The work is to cut a key for each line of the floor plan, and then to walk the building and try every door, because a key cut for the wrong room works just as well.

## The scenario

A bank runs a shop front, a payments api, a database and an operators' admin pod in three namespaces. The design lists three allowed conversations and nothing else. The demo applies policies and then tries every pair of pods.

## Run

```bash
./gradlew run
```

The demo tells the story in 5 acts, each printing exact numbers that the tests check:

| Act | What it shows |
| --- | --- |
| 1. A flat network | With no policy 20 of 20 pairs of pods reach each other; the design allows 3. |
| 2. Default deny, then the design | One deny closes all 20 pairs, three allows open exactly the 3 in the design. |
| 3. Mistakes the API server accepts | An allow for a whole namespace, a typo in a label and a forgotten pod are all valid, and only the measurement shows them. |
| 4. Egress and DNS | Denying egress stops the name lookup until DNS is allowed, and stops a call to a server outside the cluster. |
| 5. The label is the credential | A pod relabelled app=web is let in by the web's allow. |

## Test

```bash
./gradlew test
```

12 tests in `DemoRunsTest`, `IntentTest`, `PoliciesTest`. The design comparison and the policy manifests are tested with no tools. The demo makes a cluster (./gradlew integrationTest).

## What the simulation got right, and what it left out

The cluster, the policies and the network plugin are real, and the pods are small web servers. The demo prints counts and pod names, never addresses or ids.

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
| [Kubernetes Network Policies, explained](docs/kubernetes-network-policies-explained.md) | the acts in prose |
| [Session guide](docs/session.md) | a one-hour lesson with exercises |
| [Animated walkthrough](docs/animation.html) | the acts step by step, narrated |
| [Threat model](docs/threat-model.md) | what is protected, who could harm it, and how, using STRIDE |
| [Spec](docs/spec.md) | what this project must be true of |
| [Expected output](docs/expected-output.txt) | the golden demo output every test and every port checks against |

### The idea in one picture

A policy selects pods; the plugin enforces it.

![The parts](docs/images/architecture-diagram.png)

### Where each piece sits

Pods, the design and the lab.

![The classes and what they hold](docs/images/class-diagram.png)

### How the data moves

Every pod tries every other pod.

![From a probe to a matrix](docs/images/data-flow-diagram.png)

### Who calls whom, in order



![A blocked request](docs/images/sequence-diagram.png)

### Video

`video/kubernetes-network-policies-explained.mp4` (with `.m4a` audio and `.srt` subtitles) is built by `video/build_video.sh`. Rendered media is not committed.

## What it costs

- **A policy that selects nothing is valid,** and does nothing.
- **Labels become credentials,** and whoever can set them chooses the policy.
- **Egress rules need DNS too,** or names stop working.
- **Enforcement depends on the network plugin;** some accept policies and ignore them.
- **Every pair must be tested** to know what the policies do.

## When this is too much

A cluster with one team and no sensitive data can start with namespaces and RBAC. Use network policies when a taken-over pod must not reach the database, or when a rule says which service may call which.

## Where you have already met this

- A cluster where every pod can reach the database.
- A policy copied from a blog post and never tested.
- An egress rule that broke DNS on a Friday.
- A label changed to get a pod working.

## Where this sits

This is the second project of the [kubernetes-and-container-security](..) module, after [kubernetes-admission-control](../kubernetes-admission-control). The same measuring of a design against a network was done with Docker networks in [network-isolation](../../cloud-security-without-a-cloud/network-isolation).
