# Linux Hardening with Docker

```
src/main/java/com/jk/explore/security/hostscan/
├── Docker.java        The docker command, as a program
├── Finding.java       One thing the scan found: how bad it is, which rule, where, and why it matters
├── Host.java          A running container from one of the images, removed on close
├── HostScanDemo.java  Builds the two images, starts containers from them and prints the story in five numbered acts
├── Images.java        The two images of the lesson, built from Dockerfiles written here: a typical one with the usual mistakes, and one that was hardened in its recipe
├── Listeners.java     Reads the kernel's socket tables, /proc/net/tcp and /proc/net/tcp6
├── Scanner.java       Reads a running container the way an auditor reads a host: one script run as root inside it, and rules over what it printed
└── Snapshot.java      What the scanning script printed, split at its #section lines, with the views a person reads first
```

**A script run as root inside a real container reads its file modes, accounts, sshd_config, listening ports and kernel settings; a typical image has 19 findings, a machine can fix the 7 permission ones, but a fix inside a running container is gone in the next one, so the decisions belong in the image's recipe, which gives a hardened image with 0 findings; a container cannot change the kernel it shares, and how it is started is a separate matter.**

The plain [linux-hardening](../linux-hardening) project checks a prepared directory tree that stands in for a container image. This one checks **real containers**: it builds two Docker images from Dockerfiles written by the demo (`alpine:3.24.3` underneath), starts them, and runs one script as root inside each to read the files and modes, accounts, `sshd_config`, listeners and kernel settings. It needs Docker; the tests are tagged `integration`: `./gradlew integrationTest`.

What it shows:
- **A typical image.** 19 findings: an account with no password and a second UID 0 account, a copied shell with the set-user-id bit, a world-writable upload folder and file, a shadow file, a host key and a config that are too open, a group-writable cron script, root login and empty passwords in `sshd_config`, and three listeners on every interface (telnet, an unauthenticated Docker API, Redis).
- **What a machine can fix, and where the fix lives.** Seven are permission changes. They are applied in the running container; `docker diff` lists them in its writable layer; **a new container from the same image has all 19 again.**
- **The decisions, in the recipe.** A hardened Dockerfile: no second root, no test account, a locked service account, private modes, no copied shell, one listener on 8080, the process runs as `app`, and the container started with `accept_redirects` 0 and `syncookies` 1: **0 findings**.
- **Running it again.** Loosen the config inside a running hardened container and the scan finds the drift; a new container has none.
- **The bill.** A container shares the host's kernel: `kernel.randomize_va_space` cannot be changed from inside (read-only file system). Allow-lists are the shop's decisions, and how a container is started is the subject of [container-hardening](../container-hardening).

**Maps to:** OWASP Top 10 (2025) A02 Security Misconfiguration · CIS Benchmarks for Linux and Docker · CWE-732 (incorrect permission assignment), CWE-250 (execution with unnecessary privileges), CWE-1188.

## The idea in everyday terms

A building inspector who checks a flat in a block and tightens a loose handrail on the spot. When the tenants move out the handrail is loose again, because it was the flat that was fixed and not the plans. The fix that lasts is the one made in the plans.

## The scenario

An online store ships its server as a container image. The demo builds the typical image that nobody reviewed and the same shop with its decisions made, and scans both.

## Run

```bash
./gradlew run
```

The demo tells the story in 5 acts, each printing exact numbers that the tests check:

| Act | What it shows |
| --- | --- |
| 1. What the scan finds in a typical image | One script run as root inside shop-host:typical reads UID 0 for root and toor, an empty password for test, a set-user-id /tmp/.hidden-shell, world-write 777 and 666, modes 644 on the shadow file, host key and config, 775 on cron, four sshd settings at yes, accept_redirects 1 and tcp_syncookies 0, and three listeners on :: (0x0017 = 23, 0x0947 = 2375, 0x18EB = 6379). The report: 7 critical + 8 high + 2 medium + 2 low = 19; 7 auto, 12 = 3 accounts + 4 sshd + 2 kernel + 3 ports for a person. |
| 2. The fixes a machine can make, and where they live | Seven fix commands (chmod u-s, g-w, 640, 600, o-w) run as root in the running container: 19 - 7 = 12 findings, none a permission. docker diff lists the seven changed files in the container's own layer, plus 8 parent folders. A new container from the same image has 19 findings again. |
| 3. The decisions a person makes, in the recipe | The hardened recipe removes toor and test, gives app the shell /sbin/nologin, sets modes 600 and 750, sets the four sshd settings to no, listens on 8080 only, runs as USER app and is started with accept_redirects=0 and tcp_syncookies=1. Read the same way: UID 0 only root, no empty password, no set-user-id, nothing world-writable, 640, 600, 600, 750, four sshd no, sysctl 0 and 1, :: port 8080. 0 findings; the process is root in the typical image and app in the hardened one. |
| 4. Running it again | A running hardened container has 0 findings; after chmod 666 /opt/app/config.properties inside it, the same scan finds loose-permissions and world-writable-file, both HIGH: 0666 & ~0600 = 0066, rw- for the group and others. A new container from the image has 0 findings. |
| 5. The bill | Inside the container kernel.randomize_va_space reads 2, the host's value, and writing 0 is refused: read-only file system. A container shares the host's kernel; the scan ran as root; the allow-lists are the shop's; how a container is started is the next project. |

## Test

```bash
./gradlew test
```

18 tests in `ContainerScanTest`, `DemoRunsTest`, `ReadingTest`, `ScannerTest`. The rule tests run on text and need no container. The scan tests build the two images and start containers, and remove them (./gradlew integrationTest). They need Docker.

## What the simulation got right, and what it left out

The images are real, built from Dockerfiles written here on a pinned Alpine; the containers are real, with their own namespaced kernel settings; the scan is one script run through docker exec. Container names are never printed; the demo takes about half a minute.

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
| [Linux Hardening with Docker, explained](docs/linux-hardening-with-docker-explained.md) | the acts in prose |
| [Session guide](docs/session.md) | a one-hour lesson with exercises |
| [Animated walkthrough](docs/animation.html) | the acts step by step, narrated |
| [Threat model](docs/threat-model.md) | what is protected, who could harm it, and how, using STRIDE |
| [Spec](docs/spec.md) | what this project must be true of |
| [Expected output](docs/expected-output.txt) | the golden demo output every test and every port checks against |

### The idea in one picture

A recipe makes an image; a container is a copy; the scan reads the copy.

![The parts](docs/images/architecture-diagram.png)

### Where each piece sits

Images and containers on one side, rules on the other.

![The classes and what they hold](docs/images/class-diagram.png)

### How the data moves

A fix in the copy is lost with the copy.

![Where a fix lives](docs/images/data-flow-diagram.png)

### Who calls whom, in order



![A scan](docs/images/sequence-diagram.png)

### Video

`video/linux-hardening-with-docker-explained.mp4` (with `.m4a` audio and `.srt` subtitles) is built by `video/build_video.sh`. Rendered media is not committed.

## What it costs

- **The scan needs a rule for each setting,** and each rule needs an allow-list that someone owns.
- **A running container is a copy:** fixing it fixes nothing that lasts.
- **The kernel is the host's,** so some checks cannot be answered by the image.
- **Hardening can break the application,** so the scan runs where the tests do.
- **Images age:** new packages and new files bring new findings.

## When this is too much

A throw-away container that runs one trusted command for a few seconds does not need a scan. Scan images that run for long, accept connections or hold data.

## Where you have already met this

- A `USER` line, or its absence, in a Dockerfile.
- `chmod 777` in a deployment script to "make it work".
- A CIS benchmark report.
- `docker exec` used to fix something in production.

## Where this sits

This is the tools version of [linux-hardening](../linux-hardening), in the [platform-hardening](..) module. How the container is started, with which user, capabilities and mounts, comes next in [container-hardening](../container-hardening).
