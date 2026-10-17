"""The documents derived from a built project: thumbnail, YouTube sheet and spec.

Nothing here uploads anything. docs/youtube.md is the text a person pastes into
YouTube Studio, with the rules of the upload spec already applied.
"""

import json
import re
import subprocess
from pathlib import Path

from PIL import Image

REPO_PATH = "github.com/jk-boot-up/security/tree/main/java/beginner"


def clean(text):
    """No URL schemes, no angle brackets: YouTube refuses both in a description."""
    text = re.sub(r"https?://", "", text)
    return text.replace("<", "").replace(">", "")


def thumbnail(pdir):
    """docs/thumbnail.png: the poster slide, scaled to 1280x720."""
    src = pdir / "video" / "build" / "01-poster.png"
    if not src.exists():
        return None
    Image.open(src).convert("RGB").resize((1280, 720), Image.LANCZOS).save(pdir / "docs" / "thumbnail.png", optimize=True)
    return pdir / "docs" / "thumbnail.png"


def timeline(pdir):
    path = pdir / "video" / "build" / "timeline.json"
    return json.loads(path.read_text()) if path.exists() else None


def mmss(seconds):
    s = int(seconds)
    return "%02d:%02d" % (s // 60, s % 60)


def chapters(scenes, durations):
    """Chapter lines timed from the built video; the first starts at 00:00."""
    out, t = [], 0.0
    for sc in scenes:
        title = sc["title"]
        if sc["key"].endswith("poster"):
            title = "Introduction"
        out.append("%s %s" % (mmss(t), title))
        t += durations[sc["key"]]
    return out


def video_length(durations):
    return sum(durations.values()) if durations else None


def youtube_doc(d, pdir, scenes):
    p = d["project"]
    durations = timeline(pdir)
    desc = " ".join(clean(p["youtube_description"]).split())
    repo = "%s/%s/%s" % (REPO_PATH, p["module"], p["slug"])
    lines = ["# YouTube sheet — %s" % p["title"], "",
             "Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video "
             "and asks for it.**", "",
             "## Title", "", p["youtube_title"], "",
             "## Description", "", desc, "", "Source code and notes: " + repo, ""]
    if durations:
        lines += ["Chapters:", "", "```", *chapters(scenes, durations), "```", ""]
    else:
        lines += ["Chapters are added after the video is built.", ""]
    tags = p["youtube_tags"]
    lines += ["## Tags", "", ", ".join(tags), "",
              "## Files", "",
              "| What | File |", "| --- | --- |",
              "| Video | `video/%s-explained.mp4` |" % p["slug"],
              "| Captions | `video/%s-explained.srt` |" % p["slug"],
              "| Thumbnail | `docs/thumbnail.png` (1280×720) |", "",
              "## Settings", "",
              "Public with instant Premiere; not made for kids; no paid promotion; altered content: No; "
              "category Science & Technology; language English; playlist: Security Foundations in Java - "
              "Without Tools. Never announce a next video.", "",
              "## Checks before saving", "",
              "- [ ] description has no `http://`, `https://`, `<` or `>` — %s" % (
                  "ok" if not re.search(r"https?://|[<>]", desc) else "FIX"),
              "- [ ] title is at most 100 characters — %s (%d)" % ("ok" if len(p["youtube_title"]) <= 100 else "FIX",
                                                                  len(p["youtube_title"])),
              "- [ ] tags are at most 500 characters in total — %s (%d)" % (
                  "ok" if len(",".join(tags)) <= 500 else "FIX", len(",".join(tags))),
              "- [ ] description is at most 5,000 characters — %s (%d)" % ("ok" if len(desc) <= 5000 else "FIX", len(desc)),
              "- [ ] chapters start at 00:00 and there are at least three", ""]
    (pdir / "docs" / "youtube.md").write_text("\n".join(lines))
    return "docs/youtube.md"


def ffprobe_seconds(path):
    try:
        out = subprocess.run(["ffprobe", "-v", "error", "-show_entries", "format=duration", "-of",
                              "default=nokey=1:noprint_wrappers=1", str(path)],
                             capture_output=True, text=True, check=True).stdout
        return float(out.strip())
    except (OSError, subprocess.CalledProcessError, ValueError):
        return None


def spec_doc(d, pdir, tests, demo_lines):
    p, s = d["project"], d["spec"]
    video = pdir / "video" / ("%s-explained.mp4" % p["slug"])
    seconds = ffprobe_seconds(video) if video.exists() else None
    out = ["# Spec — %s" % p["title"], "", "## Purpose", "", s["purpose"].strip(), "",
           "## Not in scope", "", "\n".join("- " + n for n in s["nongoals"]), "",
           "## The problem", "", s["problem"].strip(), "",
           "## The pieces", "", "| Role | Class |", "| --- | --- |",
           *["| %s | %s |" % tuple(r) for r in s["roles"]], "",
           "## Requirements", "", "\n".join("%d. %s" % (i, r) for i, r in enumerate(s["requirements"], 1)), "",
           "## Measured", "",
           "| What | Value |", "| --- | --- |",
           "| Language and build | Java 25, Gradle wrapper |",
           "| Tests | %d, all passing |" % tests,
           "| Demo output | %d lines, identical on every run |" % demo_lines,
           "| Acts | %d |" % len(d["act"]),
           "| Video | %s |" % (("%s, 1920×1080, narrated in the amy-slow voice" % mmss(seconds)) if seconds
                                else "not built yet")]
    out += ["", "## Files every project has", "",
            "`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, "
            "explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and "
            "`animation.html`; `concept.toml`; `video/` with the scene script and voice settings.", ""]
    (pdir / "docs" / "spec.md").write_text("\n".join(out))
    return "docs/spec.md"
