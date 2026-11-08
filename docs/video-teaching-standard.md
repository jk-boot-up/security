# Video and animation teaching standard (all levels)

This standard applies to every project's video and animation: beginner, intermediate and advanced. It was set while
reworking `java/beginner/cryptography/symmetric-encryption`, which is the reference project: read its `concept.toml`,
`video/figures.py` and demo before reworking another one.

The aim: a beginner who watches the video understands the concept from concrete examples, not from abstract statements.

## 1. Pictures with real examples

- Every act has a picture (`figure`) shown before the program's output, with its own narration (`figure_speech`).
- One standalone picture early in the video shows the core idea on a single real example (for symmetric encryption:
  what AES does to one 16-byte line, character by character and byte by byte).
- Pictures are drawn by the project's `video/figures.py` with the `videokit.slides.draw` primitives, and every value
  is parsed from `docs/expected-output.txt`, so a picture can never disagree with the code.

## 2. Input and result in separate sections

- The input of each step and its result are shown in separate, labelled sections, never merged into one line or one
  sentence. Use the natural pair for the concept: BEFORE ENCRYPTION / AFTER ENCRYPTION, BEFORE HASHING / AFTER
  HASHING, THE REQUEST / THE RESPONSE, BEFORE THE FIX / AFTER THE FIX.
- This holds in the pictures (coloured header pills), in what the demo prints (section header lines, coloured
  through `[highlight]`) and in the narration (the input first, then the result).

## 3. How derived values are made

- Any value a beginner would wonder about is worked out step by step with the real values: for example how a GCM tag,
  a salted hash, a TOTP code, a signature or a certificate chain check is made.
- When the library hides the steps, a small teaching class redoes them by hand and a test proves it gives the
  library's exact result (as `TagSteps` does for the GCM tag).

## 4. Every acronym spelled out

- Every technical acronym or abbreviation used in the video or the animation is spelled out at least once: AES,
  ECB, GCM, XOR, HTTP, TLS, DNS, JWT, RBAC, CVE, NIST, UTF-8, hex and so on.
- Listed as `[[acronym]]` entries (`short`, `long`, `say`) and shown by a `kind = "acronyms"` scene after "The Words";
  the animation shows them as its first step. The first spoken use in the opening is spelled out too.

## 5. A short tour of the Java classes

- After "How To Use It", a `kind = "classes"` scene: an overview slide of all classes, then for each class a one-line
  description and only the lines of code that matter, with two to four sentences of narration.
- Keep the code as simple and short as possible, to keep the video short, but never drop the core details of the
  classes and the concept.
- Excerpts are real code from `src/main`: the generator checks every shown line against the source (whitespace
  ignored, `...` marks skipped code, `//` notes are allowed), so the slides cannot drift from the code.

## 6. Readable output slides

- Demo output lines at most about 66 characters, and at most about 13 lines per output slide; a longer act is split
  over slides with a `["---", ""]` console entry and a matching `[[page]]` in its speech.

## 7. The animation carries the same content

- `docs/animation.html` gets the acronyms step, every act with its picture above the output, and the class tour,
  all with narration, the theme picker and Play / Pause (`videokit.sh animation <project>` after `conceptkit.sh docs`).

## 8. Narration

- Spoken acronyms are spelled with spaces ("A E S", "H T T P S"); numbers that are read aloud are words; sentences
  are short and plain, separated by `[[slnc N]]` pauses.
- Rough lengths: a picture's speech 60 to 90 seconds, an output slide's 20 to 40 seconds, a class part's 15 to 25 seconds.

## 9. Working locally, to keep the work cheap

The general rules for every session are in [CLAUDE.md](../CLAUDE.md); this section is the video part.

Most of the work is done by local tools, so a person or an AI assistant reviewing a project reads short reports
and a few images rather than every file and every slide:

- `tools/conceptkit/conceptkit.sh check <project>` runs the unit tests, `docs` and `slides`, then checks this
  standard (a picture per act, the acronyms, classes and acts scenes, console line lengths and lines per slide,
  capitalised words without an `[[acronym]]` entry) and prints only the problems. It also writes contact sheets of
  twelve slides each to `<project>/build/sheets/`; review those instead of opening slides one by one.
  `--no-render` skips the tests and rendering and only checks.
- Values come from the demo's golden output by code (`video/figures.py` parses `docs/expected-output.txt`), never
  copied by hand.
- Videos are built one at a time with `conceptkit.sh build <project>` (CPU-heavy; voice clips are cached); content
  work on several projects can go on in parallel, at most three at a time.
- Do not read build logs, generated HTML or rendered slides in full; read the check report and the sheets.

## 10. Acceptance

A project meets this standard when sections 1 to 8 hold, `conceptkit.sh check` reports no problems (or only ones explained), `./gradlew test` passes, every rendered slide has been
looked at (no overlap, nothing cut off, readable text, values matching the golden output), and the author has
watched the video before anything is uploaded.
