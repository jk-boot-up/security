"""Turn a project's concept.toml into its README, docs, diagrams, animation and scenes.

Every sentence comes from concept.toml; this module only lays it out, the same
way for every project, so the teaching text is written once and nothing is
copied by hand between files.
"""

import json
import re
import tomllib
from pathlib import Path

from . import diagrams

JUNIT = "5.10.2"
JAVA = "25"
VOICE = "Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice)"



# --------------------------------------------------------- tech stack ----

ROLES = {
    "org.springframework:spring-webmvc": "Spring MVC: maps web addresses to controller methods",
    "org.springframework:spring-test": "MockMvc, which sends requests through the real filter chain without a network port",
    "org.springframework:spring-core": "Spring's utility classes and logging, which the crypto module expects the application to supply",
    "org.springframework.security:spring-security-web": "Spring Security's filter chain",
    "org.springframework.security:spring-security-config": "Spring Security's configuration, `@EnableMethodSecurity` and `HttpSecurity`",
    "org.springframework.security:spring-security-crypto": "password encoders: bcrypt, PBKDF2, scrypt, Argon2",
    "jakarta.servlet:jakarta.servlet-api": "the servlet API that Spring MVC and Spring Security are written against",
    "com.google.crypto.tink:tink": "Google Tink: safe cryptographic primitives",
    "org.bouncycastle:bcprov-jdk18on": "Bouncy Castle: algorithms and providers the JDK lacks",
    "org.bouncycastle:bcpkix-jdk18on": "Bouncy Castle PKIX: PEM, X.509 certificates and CMS",
    "com.eatthepath:java-otp": "java-otp: HOTP and TOTP one-time codes",
    "commons-codec:commons-codec": "Apache Commons Codec: Base32 and other encoders",
    "org.junit.jupiter:junit-jupiter": "JUnit 5: the tests",
    "org.junit.jupiter:junit-jupiter-params": "JUnit 5: parameterized tests",
    "org.junit.platform:junit-platform-launcher": "JUnit platform launcher, which Gradle needs to run the tests",
}


def _coords(text):
    """Dependencies and platforms from a build.gradle: (kind, group, artifact, version or None)."""
    found = []
    for kind, coord in re.findall(r"(platform|implementation|testImplementation|testRuntimeOnly|compileOnly|runtimeOnly)\s*\(?\s*'([^']+)'", text):
        parts = coord.split(":")
        found.append((kind, parts[0], parts[1], parts[2] if len(parts) > 2 else None))
    return found


def stack_rows(pdir, d):
    """The tech stack of a project, read from its own build files, as rows of [name, version, role]."""
    gradle = (pdir / "build.gradle").read_text()
    java = re.search(r"JavaLanguageVersion\.of\((\d+)\)", gradle)
    rows = [["Java", java.group(1) if java else JAVA, "the language and runtime; a Gradle toolchain finds or downloads the JDK"],
            ["Gradle", gradle_version(pdir) + " (wrapper)", "build and run; nothing to install, the wrapper downloads it"]]
    found = _coords(gradle)
    boms = {a: v for k, g, a, v in found if k == "platform" and v}
    bom_by_group = {"org.springframework": "spring-framework-bom", "org.springframework.security": "spring-security-bom",
                    "org.springframework.boot": "spring-boot-dependencies", "org.junit.jupiter": "junit-bom", "org.junit.platform": "junit-bom"}

    def bom_for(group, artifact):
        name = bom_by_group.get(group)
        return (name, boms[name]) if name in boms else None
    for kind, group, artifact, version in found:
        if kind == "platform":
            continue
        role = ROLES.get("%s:%s" % (group, artifact), "library the code uses")
        label = "%s:%s" % (group, artifact)
        if version is None:
            via = bom_for(group, artifact)
            version = ("%s, set by %s" % (via[1], via[0])) if via else "set by the build"
        if kind in ("testImplementation", "testRuntimeOnly"):
            role = role + " (tests only)"
        rows.append([label, version, role])
    for t in d.get("readme", {}).get("technologies", []):
        rows.append(list(t))
    rows.append(["videokit", "repository tool (`tools/videokit`)", "the narrated video and animation: " + VOICE])
    return rows


def stack_table(pdir, d):
    lines = ["| Technology | Version | Used for |", "| --- | --- | --- |"]
    lines += ["| %s | %s | %s |" % tuple(r) for r in stack_rows(pdir, d)]
    return lines

# ------------------------------------------------------------ helpers ----

def load(project_dir):
    return tomllib.loads((Path(project_dir) / "concept.toml").read_text())


def ul(items):
    return "\n".join("- " + i for i in items)


def ol(items):
    return "\n".join("%d. %s" % (n, i) for n, i in enumerate(items, 1))


def para(text):
    return text.strip() + "\n"


def java_tree(pdir, package):
    """The source layout with each file's one-line purpose, from its Javadoc."""
    src = pdir / "src" / "main" / "java" / "com" / "jk" / "explore" / "security" / package
    rows = []
    files = sorted(src.rglob("*.java"), key=lambda f: (len(f.relative_to(src).parts) > 1, str(f.relative_to(src))))
    for f in files:
        m = re.search(r"/\*\*(.*?)\*/", f.read_text(), re.S)
        doc = " ".join(m.group(1).replace("*", " ").split()) if m else ""
        first = re.split(r"(?<=[.!?])\s", doc, maxsplit=1)[0].rstrip(".")
        rows.append((str(f.relative_to(src)), first))
    width = max((len(n) for n, _ in rows), default=10) + 2
    lines = ["src/main/java/com/jk/explore/security/%s/" % package]
    for i, (n, w) in enumerate(rows):
        lines.append("%s %s%s" % ("└──" if i == len(rows) - 1 else "├──", n.ljust(width), w))
    return "```\n" + "\n".join(lines) + "\n```"


def test_count(pdir):
    tests = list((pdir / "src" / "test").rglob("*.java"))
    n = sum(len(re.findall(r"@(?:Test|ParameterizedTest|RepeatedTest)\b", f.read_text())) for f in tests)
    return n, sorted(f.stem for f in tests)


def gradle_version(pdir):
    props = (pdir / "gradle" / "wrapper" / "gradle-wrapper.properties").read_text()
    m = re.search(r"gradle-([\d.]+)-", props)
    return m.group(1) if m else "wrapper"


# ------------------------------------------------------------- README ----

def readme(d, pdir):
    p, r = d["project"], d["readme"]
    slug, title = p["slug"], p["title"]
    n_tests, test_classes = test_count(pdir)
    acts = d["act"]
    diagram_md = {x["name"]: x for x in d["diagram"]}

    def embed(name, caption):
        return "![%s](docs/images/%s.png)" % (caption, name) if name in diagram_md else ""

    out = ["# %s" % title, "", java_tree(pdir, p["package"]), "",
           "**%s**" % p["bold"], "", para(r["intro"])]
    if p.get("domain_note"):
        out += ["> **Why this domain:** " + p["domain_note"].strip(), ""]
    out += ["## The idea in everyday terms", "", para(r["analogy"]),
            "## The scenario", "", para(r["scenario"]),
            "## Run", "", *([para(r["run_note"])] if r.get("run_note") else []),
            "```bash", "./gradlew run", "```", "",
            "The demo tells the story in %d acts, each printing exact numbers that the tests check:" % len(acts), "",
            "| Act | What it shows |", "| --- | --- |"]
    out += ["| %d. %s | %s |" % (i, a["title"], a["summary"]) for i, a in enumerate(acts, 1)]
    out += ["", "## Test", "", "```bash", "./gradlew test", "```", "",
            ("%d tests in %s. " % (n_tests, ", ".join("`%s`" % c for c in test_classes)))
            + " ".join(r.get("test_note", "Every number the demo prints is asserted, and nothing depends on the "
                                          "clock, so every run gives the same result.").split()), "",
            *(["## What the simulation got right, and what it left out", "", para(r["twin"])] if r.get("twin") else []),
            "## Technologies and versions", "",
            "Every version below is pinned in the project's build files and was the latest stable release when the project was built.", "",
            *stack_table(pdir, d)]
    out += ["", "## Learning Material", "",
            "| Document | What it is for |", "| --- | --- |",
            *(["| [Dependencies](docs/dependencies.md) | what the framework and infrastructure are, and why they are here |"]
              if "dependencies" in d.get("docs", {}) else []),
            "| [Problem statement](docs/problem-statement.md) | the situation and what the project must show |",
            "| [Prerequisites](docs/prerequisites.md) | what you need to know first |",
            "| [%s, explained](docs/%s-explained.md) | the acts in prose |" % (title, slug),
            "| [Session guide](docs/session.md) | a one-hour lesson with exercises |",
            "| [Animated walkthrough](docs/animation.html) | the acts step by step, narrated |",
            *(["| [Threat model](docs/threat-model.md) | what is protected, who could harm it, and how, using STRIDE |"]
              if "threatmodel" in d else []),
            "| [Spec](docs/spec.md) | what this project must be true of |",
            "| [Expected output](docs/expected-output.txt) | the golden demo output every test and every port checks against |", ""]
    for name, heading in (("architecture-diagram", "The idea in one picture"),
                          ("class-diagram", "Where each piece sits"),
                          ("data-flow-diagram", "How the data moves"),
                          ("sequence-diagram", "Who calls whom, in order")):
        if name in diagram_md:
            out += ["### " + heading, "", diagram_md[name].get("caption", "").strip(), "",
                    embed(name, diagram_md[name]["title"]), ""]
    out += ["### Video", "",
            "`video/%s-explained.mp4` (with `.m4a` audio and `.srt` subtitles) is built by "
            "`video/build_video.sh`. Rendered media is not committed." % slug, "",
            "## What it costs", "", ul(r["costs"]), "",
            "## When this is too much", "", para(r["too_much"]),
            "## Where you have already met this", "", ul(r["met_before"]), "",
            "## Where this sits", "", para(r.get("where", "This project is in [%s](..)." % p["module"]))]
    return "\n".join(out).rstrip() + "\n"


# --------------------------------------------------------------- docs ----

def problem(d):
    p, x = d["project"], d["docs"]["problem"]
    return "\n".join(["# Problem Statement", "", "## The scenario", "", para(x["scenario"]),
                      "## The naive version", "", para(x["naive"]),
                      "## What this project must deliver", "", ul(x["deliver"]), ""])


def prerequisites(d, pdir):
    x = d["docs"]["prerequisites"]
    return "\n".join(["# Prerequisites", "", "## Required", "", ul(x["required"]), "",
                      "## Explicitly not required", "", ul(x["not_required"]), "",
                      "## What you will need", "",
                      "- A JDK is optional: Gradle finds or downloads JDK 25 by itself. Everything else comes with the Gradle wrapper.",
                      *(["- " + n for n in x.get("install", [])]),
                      "- About an hour for the session guide.", "",
                      "## Tech stack and versions", "",
                      "Pinned in the project's build files, and the latest stable releases when the project was built.", "",
                      *stack_table(pdir, d), ""])


def session(d):
    p, x, acts = d["project"], d["docs"]["session"], d["act"]
    per = max(5, 35 // max(1, len(acts)))
    times = ["| 0:00 | The problem and the analogy | 10 min |"]
    t = 10
    for i, a in enumerate(acts, 1):
        times.append("| 0:%02d | Act %d: %s | %d min |" % (t, i, a["title"], per))
        t += per
    times.append("| 0:%02d | Exercises | %d min |" % (t, max(10, 60 - t)))
    return "\n".join(["# Session Guide — %s" % p["title"], "", "## Learning Objectives", "",
                      "By the end of the session you can:", "", ul(x["objectives"]), "",
                      "## Timetable", "", "| Start | Topic | Time |", "| --- | --- | --- |", *times, "",
                      "## Walkthrough", "", para(x["walkthrough"]),
                      "## Exercises", "", ol(x["exercises"]), ""])


def explained(d):
    p, x, acts = d["project"], d["docs"]["explained"], d["act"]
    out = ["# %s, Explained" % p["title"], "", "## The idea in one sentence", "", para(x["sentence"]),
           "## The %d acts" % len(acts), ""]
    for i, a in enumerate(acts, 1):
        out += ["### %d. %s" % (i, a["title"]), "", para(a["detail"])]
    out += ["## The verdict", "", para(x["verdict"]),
            "## How to recognise this in code you did not write", "", ul(x["recognise"]), "",
            "## Where you have already met this", "", ul(d["readme"]["met_before"]), ""]
    return "\n".join(out)


def dependencies(d):
    """docs/dependencies.md, for tool versions of a concept."""
    x = d["docs"]["dependencies"]
    out = ["# Dependencies", "", para(x["intro"])]
    for name, text in x["what"]:
        out += ["## What %s is" % name, "", para(text)]
    out += ["## Why this project uses them", "", para(x["why"]),
            "## What to install", "", para(x.get("install_note", "Only a JDK, version 25, and a running Docker. "
                                               "Gradle downloads the rest, and the versions are pinned:")),
            "| Tool | Version |", "| --- | --- |"]
    out += ["| %s | %s |" % tuple(v) for v in x["versions"]]
    out += ["", "## What it costs", "", ul(x["costs"]), ""]
    return "\n".join(out)


def diagram_docs(d, pdir):
    """docs/images/*.png and the diagram .md files (uml-diagram.md collects the uml ones)."""
    title, docs = d["project"]["title"], pdir / "docs"
    (docs / "images").mkdir(parents=True, exist_ok=True)
    uml = []
    for x in d["diagram"]:
        diagrams.render(x, docs / "images" / (x["name"] + ".png"))
        if x["name"].startswith("uml-diagram"):
            uml.append(x)
            continue
        kind = x["name"].replace("-diagram", "").replace("-", " ").title()
        (docs / (x["name"] + ".md")).write_text("\n".join([
            "# %s — %s Diagram" % (title, kind), "", para(x.get("caption", "")),
            "![%s](images/%s.png)" % (x["title"], x["name"]), "", para(x.get("prose", ""))]))
    if uml:
        out = ["# %s — UML Sequence Diagrams" % title, ""]
        for i, x in enumerate(uml, 1):
            out += ["## %d. %s" % (i, x["title"]), "", para(x.get("caption", "")),
                    "![%s](images/%s.png)" % (x["title"], x["name"]), ""]
        (docs / "uml-diagram.md").write_text("\n".join(out))
    return len(d["diagram"])



# ------------------------------------------------------- threat model ----

STRIDE = {
    "S": "Spoofing", "T": "Tampering", "R": "Repudiation",
    "I": "Information disclosure", "D": "Denial of service", "E": "Elevation of privilege",
}


def threat_model(d):
    """docs/threat-model.md: scope, assets, actors, trust boundaries, and a STRIDE table of threats."""
    p, t = d["project"], d["threatmodel"]
    rows = d["threat"]
    out = ["# Threat Model — %s" % p["title"], "",
           "A threat model answers four questions: what are we protecting, who or what could harm it, how, "
           "and what do we do about it. This one covers this project's own demo system, not a production "
           "deployment. Domain: %s." % p.get("domain", "an online store"), "",
           "## Scope", "", para(t["scope"]),
           "## What we protect (assets)", "", ul(t["assets"]), "",
           "## Who and what acts on the system", "", ul(t["actors"]), "",
           "## Trust boundaries", "",
           "A trust boundary is a line that data or a request crosses between parts that trust each other "
           "differently. Threats cluster where those lines are.", "", ul(t["boundaries"]), "",
           *(["![Threat model diagram](images/threat-model-diagram.png)", "",
              "Each dashed box is a trust boundary. A rose tag such as T1 shows where the threat of that "
              "number, in the table below, applies.", ""]
             if any(x["name"] == "threat-model-diagram" for x in d["diagram"]) else
             ["See the picture in [the architecture diagram](architecture-diagram.md).", ""]),
           "## Threats, using STRIDE", "",
           "STRIDE is a checklist of six kinds of threat: " + ", ".join(
               "**%s** (%s)" % (STRIDE[k], k) for k in "STRIDE") + ".", "",
           "| Id | Kind | Threat | Where it shows up | Control in this project | What is left |",
           "| --- | --- | --- | --- | --- | --- |"]
    for r in rows:
        out.append("| %s | %s | %s | %s | %s | %s |" % (
            r["id"], STRIDE[r["stride"]], r["threat"], r["where"], r["control"], r["residual"]))
    out += ["", "## Assumptions", "", ul(t["assumptions"]), "",
            "## Out of scope", "", ul(t["out_of_scope"]), "",
            "## How the tests check this", "", ul(t["tests"]), "",
            "## Where this model goes next", "", para(t["next"])]
    return "\n".join(out)

# ---------------------------------------------------------- animation ----

def animation(d, pdir):
    p, a = d["project"], d["animation"]
    template = (Path(__file__).with_name("animation_template.html")).read_text()
    boxes = "\n".join(
        '      <div class="row" id="row-%s">\n        <div class="name">%s</div>\n'
        '        <div class="job">%s</div>\n      </div>' % (b[0], b[1], b[2]) for b in a["boxes"])
    steps = [dict(label="Act %d — %s" % (i, s["title"]), narration=_spoken(s),
                  rows=s.get("rows", {}), console=[list(c) for c in s.get("console", [])])
             for i, s in enumerate(d["act"], 1)]
    html = (template.replace("__TITLE__", p["title"]).replace("__SUBTITLE__", a["subtitle"])
            .replace("__BOXES__", boxes).replace("__STEPS__", json.dumps(steps, indent=1, ensure_ascii=False))
            .replace("__ROW_IDS__", json.dumps([b[0] for b in a["boxes"]])))
    (pdir / "docs" / "animation.html").write_text(html)
    return len(steps)


# ------------------------------------------------------------- scenes ----

ORDINAL = ["One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight"]


def _spoken(act):
    """The animation's words for an act: its own narration, or its video speech without pauses."""
    if act.get("narration"):
        return act["narration"]
    return " ".join(re.sub(r"\[\[slnc \d+\]\]", " ", act["speech"]).split())


def _act_scenes(d):
    """One console slide per act, built from what the demo printed and the act's speech."""
    out = []
    for i, a in enumerate(d["act"], 1):
        lines = ["%s. %s." % (ORDINAL[i - 1].upper(), a["title"])] + ["  " + c[0] for c in a.get("console", [])]
        out.append(dict(key="act-%d" % i, kind="console", title="Act %s — %s" % (ORDINAL[i - 1], a["title"]),
                        body="\n".join(lines), narration=a["speech"]))
    return out


def _expand(d):
    """[[scene]] list with a kind = "acts" marker replaced by one slide per act, keys renumbered."""
    out = []
    for s in d["scene"]:
        out += _act_scenes(d) if s["kind"] == "acts" else [s]
    for n, s in enumerate(out, 1):
        s["key"] = "%02d-%s" % (n, re.sub(r"^\d+-", "", s["key"]))
    return out


def scenes(d, pdir):
    p = d["project"]
    d = dict(d, scene=_expand(d))
    lines = ['"""Scene definitions for the %s teaching video (generated from concept.toml)."""' % p["title"],
             "", "SCENES = ["]
    for s in d["scene"]:
        body = s.get("body")
        lines.append("    dict(key=%r, kind=%r, title=%r,\n         body=%r,\n         narration=%r),"
                     % (s["key"], s["kind"], s["title"], body, " ".join(s["narration"].split())))
    poster = dict(d.get("poster", {}))
    for k in ("headline", "taglines"):
        if k in poster:
            poster[k] = [tuple(x) for x in poster[k]]
    video = dict(footer="%s  ·  Security Basics  ·  Java" % p["title"], poster=poster or None,
                 highlight=d.get("highlight", {}))
    lines += ["]", "", "VIDEO = %r" % video, ""]
    (pdir / "video" / "scenes.py").write_text("\n".join(lines))
    return len(d["scene"])


def video_readme(d, pdir):
    p = d["project"]
    # The first line shows above YouTube's fold, so it must name the concept
    # and say plainly what it is before any example.
    desc = p.get("youtube_description") or (
        "%s in Java: %s" % (p["title"], p["bold"]))
    desc = " ".join(desc.split())
    (pdir / "video" / "README.md").write_text("\n".join([
        "# %s — Teaching Video" % p["title"], "",
        "A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.", "",
        "| File | What it is |", "| --- | --- |",
        "| `%s-explained.mp4` | the video, 1920×1080 |" % p["slug"],
        "| `%s-explained.m4a` | audio only |" % p["slug"],
        "| `%s-explained.srt` | subtitles |" % p["slug"],
        "| `poster.png` | the opening frame |", "",
        "**Narration:** %s. Scenes are generated from `../concept.toml` into `scenes.py`." % VOICE, "",
        "Suggested description:", "", "> " + desc, ""]))


def all_docs(pdir):
    d = load(pdir)
    slug = d["project"]["slug"]
    docs = pdir / "docs"
    docs.mkdir(exist_ok=True)
    (pdir / "README.md").write_text(readme(d, pdir))
    (docs / "problem-statement.md").write_text(problem(d))
    (docs / "prerequisites.md").write_text(prerequisites(d, pdir))
    (docs / "session.md").write_text(session(d))
    (docs / ("%s-explained.md" % slug)).write_text(explained(d))
    if "dependencies" in d.get("docs", {}):
        (docs / "dependencies.md").write_text(dependencies(d))
    if "threatmodel" in d:
        (docs / "threat-model.md").write_text(threat_model(d))
    n_diag = diagram_docs(d, pdir)
    n_steps = animation(d, pdir)
    n_scenes = scenes(d, pdir) if d.get("scene") else 0
    video_readme(d, pdir)
    return "README + 4 docs, %d diagrams, %d animation steps, %d scenes" % (n_diag, n_steps, n_scenes)
