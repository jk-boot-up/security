"""Local checks against docs/video-teaching-standard.md, so a reviewer reads a short report and a few
contact sheets instead of every slide.

    tools/conceptkit/conceptkit.sh check <project> [--no-render]

Runs the unit tests, conceptkit docs and videokit slides, then prints only problems:
long or too many console lines, acronyms in the narration or slides with no [[acronym]] entry,
acts without a figure, scenes missing from the standard, and the animation's step list.
Writes contact sheets (12 slides each) to <project>/build/sheets/sheet-N.png (not committed).
"""

import re
import subprocess
import sys
import tomllib
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
# words that look like acronyms but are not jargon (demo values, section headers)
IGNORE = {"BEFORE", "AFTER", "STEP", "THE", "ONE", "TWO", "THREE", "FOUR", "FIVE", "SIX", "SEVEN", "EIGHT",
          "NO", "OK", "ID", "US", "IT", "TO", "A", "I"}


def sh(*cmd, cwd=REPO):
    r = subprocess.run([str(c) for c in cmd], cwd=cwd, capture_output=True, text=True)
    return r.returncode, (r.stdout + r.stderr)[-1500:]


def check(slug, render=True):
    pdir = next(REPO.glob("java/*/*/%s" % slug))
    problems = []
    if render:
        for cmd, cwd in ((["./gradlew", "-q", "test"], pdir),
                         (["tools/conceptkit/conceptkit.sh", "docs", slug], REPO),
                         (["tools/videokit/videokit.sh", "slides", slug], REPO)):
            for old in (pdir / "video" / "build").glob("*.png") if cmd[0].endswith("videokit.sh") else ():
                old.unlink()
            code, out = sh(*cmd, cwd=cwd)
            if code:
                print("FAILED: %s\n%s" % (" ".join(cmd), out))
                return 1
    d = tomllib.loads((pdir / "concept.toml").read_text())
    kinds = [s["kind"] for s in d["scene"]]
    for k in ("acronyms", "classes", "acts"):
        if k not in kinds:
            problems.append("no scene of kind %r" % k)
    for i, a in enumerate(d["act"], 1):
        if not a.get("figure"):
            problems.append("act %d has no figure (fine only if its output alone shows the example)" % i)
        page = 0
        for c in a.get("console", []):
            if c[0] == "---":
                page = 0
                continue
            page += 1
            if len(c[0]) > 68:
                problems.append("act %d: console line over 68 chars: %s" % (i, c[0][:50]))
        if page > 14 or len(a.get("console", [])) > 14 and not any(c[0] == "---" for c in a["console"]):
            problems.append("act %d: more than 14 console lines on one slide; split with [\"---\", \"\"] + [[page]]" % i)
    # acronyms used vs explained: only the narration and the slide text
    ns = {}
    exec((pdir / "video" / "scenes.py").read_text(), ns)
    text = " ".join(str(sc.get("narration", "")) + " " + ("" if sc.get("kind") == "code" else str(sc.get("body", "")))
                    for sc in ns["SCENES"])                         # code slides hold identifiers, not jargon
    have = set()
    for a in d.get("acronym", []):
        short = a["short"]
        have.add(re.sub(r"[^A-Z0-9]", "", short.upper()))
        have |= {w.upper() for w in re.split(r"[/,]\s*", short)}              # "GET/POST"
        have.add(re.split(r"[^A-Za-z]", short)[0].upper())
        m = re.fullmatch(r"([A-Za-z]+)(\d+)\s*[-\u2013]\s*\1?(\d+)", short)   # "R1-R7"
        if m:
            have |= {"%s%d" % (m.group(1).upper(), n) for n in range(int(m.group(2)), int(m.group(3)) + 1)}
    used = set(re.findall(r"\b[A-Z][A-Z0-9]{1,6}\b", text))
    for run in re.findall(r"\b(?:[A-Z] ){1,7}[A-Z](?=s?\b)", text):          # "A E S G C M" -> AES, GCM
        letters = run.replace(" ", "")
        while letters:
            hit = next((h for h in sorted(have, key=len, reverse=True) if letters.startswith(h)), None)
            if not hit:
                used.add(letters)
                break
            letters = letters[len(hit):]
    words = set(re.findall(r"\b[A-Z]{2,}\b", (pdir / "docs" / "expected-output.txt").read_text()))
    missing = sorted(u for u in used if u not in have and u not in IGNORE and u not in words)
    if missing:
        problems.append("capitalised words with no [[acronym]] entry (add one if it is jargon): " + ", ".join(missing))
    anim = (pdir / "docs" / "animation.html").read_text()
    labels = re.findall(r'"label": "([^"]+)"', anim)
    print("scenes: %d   animation steps: %d   figures: %s" % (
        len(list((pdir / "video" / "build").glob("*.png"))), len(labels),
        ", ".join(a["figure"] for a in d["act"] if a.get("figure"))))
    print("animation: " + " | ".join(l[:40] for l in labels))
    # contact sheets
    from PIL import Image, ImageDraw
    out = pdir / "build" / "sheets"
    out.mkdir(parents=True, exist_ok=True)
    pngs = sorted((pdir / "video" / "build").glob("*.png"))
    for k in range(0, len(pngs), 12):
        sheet = Image.new("RGB", (3 * 640, 4 * 360), "black")
        dr = ImageDraw.Draw(sheet)
        for j, p in enumerate(pngs[k:k + 12]):
            x, y = (j % 3) * 640, (j // 3) * 360
            sheet.paste(Image.open(p).resize((640, 360)), (x, y))
            dr.text((x + 6, y + 340), p.stem, fill="yellow")
        sheet.save(out / ("sheet-%d.png" % (k // 12 + 1)))
    print("sheets: " + ", ".join(str(p) for p in sorted(out.glob("sheet-*.png"))))
    print("PROBLEMS:\n  " + "\n  ".join(problems) if problems else "no problems found")
    return 0



