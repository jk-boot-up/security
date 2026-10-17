# Prerequisites

## Required

- Java basics
- A rough idea of what a pod, a namespace and a service account are

## Explicitly not required

- A running Kubernetes cluster: the demo reads manifests
- Docker, unless you run the integration tests

## What you will need

- A JDK is optional: Gradle finds or downloads JDK 25 by itself. Everything else comes with the Gradle wrapper.
- Docker, kind and kubectl for the integration tests only
- About an hour for the session guide.

## Tech stack and versions

Pinned in the project's build files, and the latest stable releases when the project was built.

| Technology | Version | Used for |
| --- | --- | --- |
| Java | 25 | the language and runtime; a Gradle toolchain finds or downloads the JDK |
| Gradle | 9.8.1 (wrapper) | build and run; nothing to install, the wrapper downloads it |
| org.snakeyaml:snakeyaml-engine | 3.2 | library the code uses |
| org.junit.jupiter:junit-jupiter | 6.1.3, set by junit-bom | JUnit 5: the tests (tests only) |
| org.junit.platform:junit-platform-launcher | 6.1.3, set by junit-bom | JUnit platform launcher, which Gradle needs to run the tests (tests only) |
| kind | 0.33.0 | makes a real Kubernetes cluster in Docker for the integration tests |
| kubectl | 1.27.2 (client) | asks the cluster `auth can-i` as each service account; newer than the supported skew is only a warning |
| Kubernetes | 1.37.0 (kindest/node image) | the cluster the manifests are applied to in the integration tests |
| Docker | required for the integration tests | runs the kind node |
| videokit | repository tool (`tools/videokit`) | the narrated video and animation: Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice) |
