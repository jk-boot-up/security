# Working in this repository (for every session and every agent)

Read this first. It applies to every Claude Code session and every agent working here, now and later.

## 1. Spend few tokens: do the work locally

The repository has local tools for nearly every mechanical step. Use them, and read their short output, instead of
reading many files or images.

- **Check a project with one command:** `tools/conceptkit/conceptkit.sh check <project>`. It runs the unit tests,
  regenerates the docs and slides, checks the teaching standard and prints only the problems. It also writes contact
  sheets, twelve slides per image, to `<project>/build/sheets/`. Look at the sheets, not at slides one by one.
  `--no-render` only checks.
- **Generate, don't hand-write:** README, docs, diagrams, animation and video scenes all come from the project's
  `concept.toml` (`conceptkit.sh docs <project>`). Edit `concept.toml`, then regenerate. Pictures are drawn by
  `video/figures.py` and read their values from `docs/expected-output.txt`, so never copy values by hand.
- **Read only what you need:** use `grep`, `sed -n`, `head` or a short Python script for the part you need. Do not
  read in full: build logs, generated HTML, `video/scenes.py`, `docs/youtube*.json`, agent transcripts or rendered
  slides.
- **Compare with scripts:** count, diff or validate with a quick local script rather than by reading two files.
- **Long jobs run in the background:** start video builds (`conceptkit.sh build <project>`) in the background and
  wait for the notification. Don't poll. Build videos **one at a time**; they are CPU-heavy on this machine.
- **Parallel work:** at most **three** agents at a time, each touching only its own project directory. Give agents a
  short brief that points to files, rather than pasting long content into the prompt.
- **Report briefly:** what changed, the test result, any problem. No long recaps.

## 2. Rules the author set

- **Never upload, schedule or publish** anything to YouTube until the author has watched the video, the animation
  and read the docs, and asks for it. Deleting a published video is irreversible. Ask first unless the author has
  asked for it.
- **Commit or push only when asked.** Commit messages and PR text never carry a Co-Authored-By line or any mention
  of an AI or assistant.
- **Never mention the sibling `java-design-patterns` project** in docs or code.
- **Domains:** every project uses an online store (e-commerce) or a bank, nothing else.
- **Local labs only:** any local tool (Docker, kind, nginx, Terraform...) is fine; no cloud account. Attacks target
  only the project's own deliberately weak code and containers.
- Latest stable versions, pinned; no deprecated API (`-Werror`). Deterministic tests; tool-dependent tests are
  tagged and run with `./gradlew integrationTest`.

## 3. Where the specs are

- `docs/video-teaching-standard.md`: how every video and animation must teach (pictures with real examples, input
  and result in separate sections, derived values worked out, every acronym spelled out, a short class tour). The
  reference project is `java/beginner/cryptography/symmetric-encryption`.
- `docs/beginner-plan.md`, `docs/intermediate-plan.md`, `docs/advanced-plan.md`: the plans per level, including the
  repository layout, voice, video and YouTube process.
- `java/learning-path.md`: the order of the projects.
