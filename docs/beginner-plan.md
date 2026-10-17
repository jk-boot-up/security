# Plan: Beginner Track

Status: **draft**, 17 October 2026.

This plan covers the **Beginner: Foundations** section of the [README](../README.md). It is
self-contained: sections 3 to 8 define the structure, tooling, audio, video, animation and YouTube
standards for this repository.

## 1. Goal

Every beginner topic becomes one or more small, runnable teaching projects. Each project has:

1. **Java 25 (JDK 25) + Gradle code** with deterministic tests.
2. **Two kinds of version where feasible:** *without tools* (plain JDK) and *with tools*. A tool
   is anything that runs on the learner's own machine: a framework or library (Spring Security,
   Spring Boot, Bouncy Castle, Tink) or local infrastructure (Docker, Docker Compose, Kubernetes
   through kind, Terraform, nginx, Keycloak, CoreDNS, MinIO, LocalStack, OpenSSL, Wireshark, Trivy
   and similar). Python is also allowed, for helper scripts, small attacker or checker tools
   (for example packet crafting with Scapy) and the repository's own tooling. A topic gets only one version when the other would add nothing. Section 4 marks
   each case.
3. **An interactive animation** in the browser, voiced step by step.
4. **A narrated video** uploaded to YouTube.

**Language plan:** Java now (plain and with tools); Go and C++ later, in the same folder
structure (section 3).

All labs run **locally**. No cloud account, no hosted lab service. Any open-source or free tool
that runs on the learner's machine is allowed. Anything that needs a download (container images,
a vulnerability database) is called out in the project's `prerequisites.md`, and the demo runs
without the internet once those are in place.

## 2. Principles

- **Beginners first.** Plain words, short sentences, one idea at a time. Every term is explained
  where it first appears.
- **Works with the eyes closed.** Narration never says "as you can see". It names the actors and
  describes the flow in order.
- **Show it broken first.** Each project runs the naive or insecure version, shows exactly how it
  fails, then the fix, then what the fix costs and where it is not enough.
- **Each topic picks the domain that teaches it best.** A small online store is the fallback
  because learners can carry it from topic to topic, but a clinic, a bank, a library or anything
  else is used where it makes the idea clearer. The chosen domain is named in the topic's
  `concept.toml` and README, and every language uses the same one. Analogies may come from anywhere.
- **Deterministic.** Fixed seeds, fixed clocks, no `Thread.sleep`, no real internet at run time.
  Every run prints the same numbers, and tests assert every number the demo prints. Tool versions
  and container images are pinned to exact versions.
- **Defensive and educational only.** Attacks are shown against code and containers in the same
  project, on the learner's own machine (loopback or a local Docker network), so the learner sees
  why the defence exists.
- **Concepts first, patterns later.** This track teaches the concepts underneath security
  patterns such as authorization policies, input validation, secrets management and token
  authentication. The patterns themselves belong to later tracks.

## 3. Repository layout

**One project per concept**, each self-contained, grouped into module folders. A with-tools
version of a concept is **its own sibling project**, not a subfolder of the plain one. The
language is the top-level folder, so Go and C++ can be added later as parallel trees with the
same module and project names. Java is first.

```
security/
  README.md
  docs/
    beginner-plan.md                     this file
    youtube-links.json                   registry of published videos (generated)
  tools/                                 scaffold, docgen, videokit, ytupload (section 5)
  java/                                  Java 25 + Gradle
    beginner/
      README.md                          the modules and the order to read them in
      foundations/                       one folder per module
        README.md                        the module's projects and how they connect
        docs/
          spec.md                        the shared feature, the forced change, the scale limits
          implementation-plan.md         the build order and rules for the module
        cia-triad/                       one project per concept
        threats-and-risk/
      identity/
        authentication-vs-authorization/
        authentication-vs-authorization-with-spring-security/
        authentication-vs-authorization-with-keycloak/
      passwords-and-mfa/
      cryptography/
      networking/
      transport-security/
      safe-habits/
  go/                                    added later, same shape
    beginner/<module>/<project>/
  cpp/                                   added later, same shape
    beginner/<module>/<project>/
```

`java/intermediate/` and `java/advanced/` will sit beside `java/beginner/` when those tracks start.

**Rules**

- **Project names:** `<concept>` for the plain version and `<concept>-with-<tool>` for each tool
  version. Names use no language word; the language comes from the top-level folder. The two
  sort next to each other, and a concept with several tools gets one project per tool.
- **Every project is self-contained**, with its own build, tests, docs, diagrams, animation,
  video scripts and (where used) its own `infra/` folder. Nothing is shared at run time, so a
  learner can copy one project out and run it.
- **Module folders** hold a README (the projects in reading order, and how each builds on the
  last) and `docs/spec.md` plus `docs/implementation-plan.md`, which fix what the module's
  projects are before any of them is written. Each module also states one shared feature or
  scenario and a forced change that its projects are judged by, where that applies.
- **Plain is always the standard library only.** A tool project depends on the tool and says so
  in its `prerequisites.md`.
- **Build files by language:** Java uses a Gradle wrapper with a JDK 25 toolchain (the build
  picks the right JDK itself; each `prerequisites.md` names the exact JDK and Gradle versions).
  Go uses `go.mod`. C++ uses `CMakeLists.txt`, with `vcpkg.json` or `conanfile.txt` for projects
  that need libraries.
- **Local infrastructure** (Docker Compose files, nginx or CoreDNS configuration, kind manifests,
  Terraform, Python helpers) lives in the project's `infra/` folder, with container images
  pinned to exact versions.
- **Tests stay fast and deterministic.** The default test task (`./gradlew test`) never needs
  Docker. Tests that need containers run through a separate `integrationTest` task that starts
  and stops the containers itself.
- **Package and module roots:** Java `com.jk.explore.security.<concept>`; Go
  `github.com/jk-boot-up/security/go/beginner/<module>/<project>`; C++ namespace
  `jk::security::<concept>`.
- **Across languages, the teaching is identical.** A concept uses the same module, project name,
  domain, scenario, acts and numbers in every language. Each project keeps its golden demo output
  in `docs/expected-output.txt`; a port copies that file and its tests compare against it. The
  first Java project fixes the golden output, and ports follow module by module once the Java
  module is finished and stable.
- **Videos are per project**, because the code on screen differs. Each project has its own
  `video/` and `youtube.md`, and playlists name the language (section 8).

**What each project holds**

| Part | Content |
| --- | --- |
| Code | the insecure version, the secure version and a demo `main` that runs the numbered acts. Java: `src/main/java`, `src/test/java` (JUnit 5). Go: package files plus `_test.go`. C++: `src/`, `tests/` |
| Build | Gradle wrapper (or `go.mod`, or CMake), so the project runs with one command |
| `README.md` + `README.html` | what it is, the analogy, the scenario, how to run and test, the technologies and versions, the diagrams, the costs, when it is not enough |
| `docs/` | `problem-statement`, `prerequisites`, `session` (one-hour guide with exercises), `<concept>-explained`, class, sequence and data-flow diagrams, `spec`, `expected-output.txt`, `youtube.md`, `thumbnail.png`, `animation.html`, `images/` |
| `infra/` | only where the project uses local tooling |
| `concept.toml` | the single content file: scenario, analogy, acts with their exact numbers, README prose, diagram descriptions, animation steps and video scenes |
| `video/` | `scenes.py`, `videokit.toml` (voice settings) |

## 4. Projects

Legend: **P** = plain JDK, **T** = with tools (frameworks, libraries or local infrastructure).
"–" means that version is not planned.

### Module 1: Foundations

| # | Project | Scenario (domain in brackets; the store is the fallback) | P | T |
| ---: | --- | --- | :---: | :---: |
| 1 | `cia-triad` | [clinic] A patient record is leaked, silently changed, and made unavailable. Each breaks one property; a control restores it | P | – |
| 2 | `threats-and-risk` | [online store] List the shop's endpoints as an attack surface, score threat × likelihood × impact, rank the risks | P | – |

### Module 2: Identity

| # | Project | Scenario | P | T |
| ---: | --- | --- | :---: | :---: |
| 3 | `authentication-vs-authorization` | Logging in proves who you are; it does not say what you may do. A customer who reaches an admin page | P | Spring Security; Keycloak in Docker for a real login server |

### Module 3: Passwords and multi-factor

| # | Project | Scenario | P | T |
| ---: | --- | --- | :---: | :---: |
| 4 | `password-hashing` | Plain-text storage, then unsalted hashes, then salted and slow hashes (PBKDF2 plain; BCrypt and Argon2 with a framework) | P | Spring Security Crypto |
| 5 | `password-attacks-and-defences` | Brute force and dictionary attacks on a tiny fixed wordlist; cost factor, rate limiting, lockout | P | nginx rate limiting in Docker |
| 6 | `totp-multi-factor` | One-time codes per RFC 6238 built from HMAC; clock drift and replay | P | a TOTP library, and a standard authenticator app against a local Keycloak |

### Module 4: Cryptography

| # | Project | Scenario | P | T |
| ---: | --- | --- | :---: | :---: |
| 7 | `hashing-and-integrity` | Detect a tampered invoice with SHA-256, then HMAC when the attacker can recompute a plain hash | P | – |
| 8 | `symmetric-encryption` | Encrypt card notes with AES-GCM; why ECB and reused nonces fail | P | Google Tink |
| 9 | `asymmetric-encryption` | Share a key without meeting: RSA and key agreement | P | Bouncy Castle |
| 10 | `digital-signatures` | Sign a price list; verify it; show a forged one fail | P | Bouncy Castle |
| 11 | `keystore-basics` | Where keys live: PKCS12 keystores, key derivation, never hard-coding a key | P | – |

### Module 5: Networking

| # | Project | Scenario | P | T |
| ---: | --- | --- | :---: | :---: |
| 12 | `tcp-ip-basics` | A loopback socket exchange; a listener on the same loopback reads a plain-text login | P | tcpdump and Wireshark; a small Python sniffer |
| 13 | `dns-basics` | A small DNS exchange against a local test server; a spoofed answer sends a customer of a bank to a fake address | P | CoreDNS or dnsmasq in Docker |
| 14 | `http-basics-and-headers` | Requests, cookies and security headers (`HttpOnly`, `Secure`, `SameSite`, HSTS) | P (`com.sun.net.httpserver`) | Spring Boot; nginx security headers in Docker |

### Module 6: Transport security

| # | Project | Scenario | P | T |
| ---: | --- | --- | :---: | :---: |
| 15 | `tls-handshake` | A client and server on loopback; each handshake step printed in order; what an eavesdropper sees before and after | P (`SSLEngine`) | OpenSSL `s_client` and `s_server`; Wireshark; a Python Scapy script to read the handshake |
| 16 | `certificates-and-chain-of-trust` | Root, intermediate and leaf; validate a chain, reject an expired or self-signed one | P (`CertPathValidator`, test keystores made with `keytool`) | Bouncy Castle; an OpenSSL-built CA |
| 17 | `https-server` | [bank] Serve a banking site over HTTPS and check what the client verifies | P (`HttpsServer`) | Spring Boot SSL bundles; nginx or Caddy TLS in Docker |

### Module 7: Safe habits

| # | Project | Scenario | P | T |
| ---: | --- | --- | :---: | :---: |
| 18 | `least-privilege` | A report job with admin rights is exploited; the same job with only the rights it needs | P | Spring Security method security; a non-root Docker container; Kubernetes RBAC on kind; Terraform with LocalStack IAM |
| 19 | `patching-and-dependency-hygiene` | A vulnerable library version in a Gradle build; find it, upgrade it, lock versions | P (Gradle dependency locking and verification) | OWASP Dependency-Check; Trivy |
| 20 | `backups-and-recovery` | Back up, verify with checksums, restore after a simulated ransomware encryption; the 3-2-1 rule | P | MinIO or LocalStack S3 as the second copy, created with Terraform |

**Total: 20 concepts, about 40 projects** (20 plain, about 20 with tools). Each tool version is its own sibling project named `<concept>-with-<tool>`; a concept with several tools gets one project per tool.

`mTLS`, the TLS-attack material and certificate lifecycle belong to the Intermediate and Advanced
tracks in the README and are out of scope here.

## 5. Tooling

This repository carries its own small toolset under `tools/`, built in Phase 0:

| Tool | Job |
| --- | --- |
| `scaffold` | create a module folder or a project: build files, package layout, voice settings, starter `concept.toml` |
| `docgen` | generate the Markdown and HTML docs, diagrams, animation page and video scenes from `concept.toml` |
| `videokit` | narration (Piper), slides, video, captions and animation audio, as one library; a project's `video/` folder holds only its content |
| `ytupload` | prepare the upload sheet; later, the scripted YouTube Studio upload and the link registry |

One command builds a project end to end: tests, docs, diagrams, animation, video and index.

The tools must support the language / track / module / project layout of section 3, with
per-language build and test commands (Gradle, `go test`, CMake with CTest) and per-language
thumbnails and playlists. They are written once and shared by every language.

## 6. Audio and video

- **Voice:** Piper `en_US-amy-medium`, speed 0.8, `sentence_gap = 0.7`, `pause_scale = 1.5`,
  −16 LUFS, peaks limited to −1.5 dBFS. Written to each project's `video/videokit.toml`.
- **Credit** is always third person: "This video is presented by Jayasekhar Konduru." No
  first-person narration.
- **"ID"** is spoken "I D". Numbers and symbols are spoken as words.
- **Opening order:**
  1. what the video is: "This video explains X, in Java.";
  2. the credit;
  3. a plain definition;
  4. the same idea taught in the topic's own domain.
- **Closing:** one-sentence lesson, the repository, a like and a subscribe, thanks. Never name
  the next video.
- **Length:** about 14 scenes per video.
- **Quality gates:** continuous audio, no first-person credit, captions timed from the recorded
  phrases. A build fails rather than ship a bad video.
- **Security-specific wording:** an attack is always described as "the attacker" acting on the
  project's own demo code. No instructions aimed at real systems.

## 7. Animation

- `docs/animation.html` per project, one step per act, generated from `concept.toml`.
- Narration in the amy-slow voice only, rendered by `videokit.sh animation <project>` into
  `docs/audio/` and injected between the `videokit:narration` markers. Audio is never embedded
  in the HTML.
- The shared UX layer: Light / Dim / Dark theme, clickable timeline, Back button, progress bar,
  **Predict first**, keyboard control, an All-steps list, no sideways scrolling at phone width,
  reduced motion respected.
- A Narration on/off toggle and a Play / Pause / Resume button for the step on screen.
- Without the audio clips, the page still works silently.

## 8. YouTube

**Files uploaded:** the `.mp4` (which already contains the narration), the matching `.srt`, and
the project's own `docs/thumbnail.png` (1280×720). The `.m4a` is never uploaded.

**Text rules:**

- No `http://` or `https://` in the description; the repository link reads
  `github.com/jk-boot-up/security/tree/main/<language>/beginner/<module>/<project>`.
- No `<` or `>` in the title or description.
- The first sentence is the name and plain definition: "X in Java: ...".
- Chapters are timed from the uploaded video's own `.srt`: start at `00:00`, at least three.
- Title at most 100 characters (aim for 60), description at most 5,000, tags at most 500.

**Settings:** Public with instant Premiere; not made for kids; no paid promotion; altered
content **No**; category Science & Technology; language English set before subtitles; captions
from the `.srt`; thumbnail uploaded; end screen to an already published video; never announce
a next video.

**Playlists:** per language, then two per module, never mixed. The tables below are the Java
playlists. Go and C++ get the same set with "in Go" or "in C++" in place of "in Java", created on
the first upload for that language.

| Module | Without tools | With tools |
| --- | --- | --- |
| 1 Foundations | Security Foundations in Java - Without Tools | n/a |
| 2 Identity | Identity Basics in Java - Without Tools | Identity Basics in Java - With Tools |
| 3 Passwords and MFA | Passwords and MFA in Java - Without Tools | Passwords and MFA in Java - With Tools |
| 4 Cryptography | Cryptography Basics in Java - Without Tools | Cryptography Basics in Java - With Tools |
| 5 Networking | Networking for Security in Java - Without Tools | Networking for Security in Java - With Tools |
| 6 Transport security | TLS and Certificates in Java - Without Tools | TLS and Certificates in Java - With Tools |
| 7 Safe habits | Safe Habits in Java - Without Tools | Safe Habits in Java - With Tools |

Videos sit in learning order. Playlist names are placeholders until the first upload.

**No uploads before review.** Nothing is uploaded, scheduled or published until the author has reviewed that project's rendered video, animation and docs and asked for it. Until then the pipeline stops at the local build: the upload step is limited to printing the upload sheet (`ytupload.sh sheet`), which sends nothing. Approval is per project, or per batch if the author says so.

**Process:** the scripted uploader drives YouTube Studio in a Chrome window the channel owner has
signed in to by hand. It never types a password and runs only when the owner asks. Uploads happen
one video at a time, a failure never causes a duplicate, and every link is recorded in
`docs/youtube-links.json`, which feeds the index.

**Never committed:** `.mp4`, `.m4a`, `.srt`, `.wav`, audio clips, build output.

## 9. Phases

### Phase 0: Set up (once)

1. Build the tools in section 5, starting with `scaffold` and `videokit`.
2. Create `java/beginner/` with its README and module folders.
3. Add a `.gitignore` for rendered media and Gradle output.
4. Add a repo-level generator for the index and the YouTube registry.
5. Build project 1 end to end as the pilot: code, docs, diagrams, animation, video, thumbnail,
   a dry-run upload sheet (no upload). Fix the pipeline before building the rest.

### Phase 1: Build the modules, one project at a time

For each project:

1. Scaffold it.
2. Write the insecure version, the secure version and the demo, in 4–6 acts with exact numbers.
3. Write the tests; run `./gradlew test run` until green and the output reads well.
4. Write `concept.toml` using the numbers the demo actually printed.
5. Generate docs, diagrams and animation; check the README, one diagram and the animation page.
6. Render the video and animation audio in the background and move on.
7. Tick the checklist.

Order: modules 1 to 7, plain version first, then the with-tools version of the same topic. This
phase is **Java only**.

### Phase 1b: More languages (later)

Per module, once its Java projects are done and their `expected-output.txt` files are stable:

1. Add the module's projects under `go/beginner/<module>/`, with the same project names, then the
   `-with-<library>` projects.
2. Copy each project's `docs/expected-output.txt`, run the demo and diff its output against it.
3. Adapt `concept.toml` for the code on screen and render the video. Upload only after review, as in Phase 2.
4. Repeat for `cpp/`.

Library candidates, to be confirmed when the time comes: Go `golang.org/x/crypto`, `crypto/tls`,
`net/http`; C++ OpenSSL, libsodium, Botan. Some projects may be skipped in a language where they add
nothing (for example Spring Security has no Go twin; the Go project would use `x/crypto` and
`net/http` middleware instead).

### Phase 2: Publish

1. Review each rendered video: first and last minute, credit line, no next-topic teaser.
2. Upload only after the author has reviewed and approved a project's video, animation and docs, and only when the author asks for uploads. Then go module by module, one video at a time, plain playlists first.
3. After each batch, regenerate the index and the registry.
4. Commit and push source only. Media stays local.

## 10. Acceptance for each project

1. `./gradlew test` passes and `./gradlew run` prints the acts.
2. All files in section 3 exist. HTML pages inline styles and link images; no base64 images; no
   Mermaid.
3. Video and animation are rendered in the amy-slow voice with no failed gate.
4. The project appears in the beginner README and the index.
5. Nothing rendered is committed.
6. The demo attacks only its own code and containers, on the learner's machine.

## 11. Checklist

| # | Project | Plain code | With-tools code | Docs | Animation | Video | Uploaded |
| ---: | --- | :---: | :---: | :---: | :---: | :---: | :---: |
| 1 | cia-triad | ☐ | – | ☐ | ☐ | ☐ | ☐ |
| 2 | threats-and-risk | ☐ | – | ☐ | ☐ | ☐ | ☐ |
| 3 | authentication-vs-authorization | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ |
| 4 | password-hashing | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ |
| 5 | password-attacks-and-defences | ☐ | – | ☐ | ☐ | ☐ | ☐ |
| 6 | totp-multi-factor | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ |
| 7 | hashing-and-integrity | ☐ | – | ☐ | ☐ | ☐ | ☐ |
| 8 | symmetric-encryption | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ |
| 9 | asymmetric-encryption | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ |
| 10 | digital-signatures | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ |
| 11 | keystore-basics | ☐ | – | ☐ | ☐ | ☐ | ☐ |
| 12 | tcp-ip-basics | ☐ | – | ☐ | ☐ | ☐ | ☐ |
| 13 | dns-basics | ☐ | – | ☐ | ☐ | ☐ | ☐ |
| 14 | http-basics-and-headers | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ |
| 15 | tls-handshake | ☐ | – | ☐ | ☐ | ☐ | ☐ |
| 16 | certificates-and-chain-of-trust | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ |
| 17 | https-server | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ |
| 18 | least-privilege | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ |
| 19 | patching-and-dependency-hygiene | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ |
| 20 | backups-and-recovery | ☐ | – | ☐ | ☐ | ☐ | ☐ |

## 12. Risks

- **Tooling is new work.** The pilot project in Phase 0 proves the whole chain before the other projects are built.
- **Plain JDK has no public certificate-creation API.** Test keystores are made with `keytool` by
  a Gradle task, so the plain version stays dependency-free; the with-tools version uses Bouncy
  Castle or OpenSSL.
- **Some demos need network features** (DNS, TLS). All run on the learner's machine, against test
  servers or containers in the same project, never the real internet.
- **Tool variants need Docker (and kind for Kubernetes).** They add setup, disk and memory cost,
  and Docker behaves slightly differently across macOS, Linux and Windows. Mitigations: a plain
  version always exists so the topic can be learned without Docker, images are pinned, container
  tests are kept out of the default test task, and `prerequisites.md` lists the exact versions
  and resources needed.
- **Dependency-hygiene tools download vulnerability data.** This needs the internet but no
  account; the prerequisites page says so, and the demo can run against a bundled data snapshot.
- **Security content ages.** Each project names the algorithm and library versions it uses, and
  the README says which choices are current as of the build date.
- **Misuse.** Attacks are demonstrated only on the project's own code, and the README's ethics
  note applies to every project.

## 13. Open questions

1. **Playlist names:** are the names in section 8 acceptable?
2. **Voice and pipeline:** keep Piper `en_US-amy-medium` for every language's videos, or choose per language?
3. **Languages and order:** is Java → Go → C++ the right order, and should a language wait until
   the whole Java track is finished, or follow module by module as in Phase 1b?
4. **Scope check:** the list stops at the README's beginner topics. Should small additions such
   as encoding vs. encryption vs. hashing, or the basics of randomness, get their own projects?
