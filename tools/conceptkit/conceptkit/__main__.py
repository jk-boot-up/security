"""conceptkit: build one concept project from its concept.toml.

    conceptkit.sh docs  <project>    README, docs/*.md, diagrams, animation.html, scenes.py
    conceptkit.sh build <project>    docs, tests, slides, thumbnail, video, animation narration,
                                     YouTube sheet, spec and the HTML twins
    conceptkit.sh build <project> --no-media    the same without the video and the narration audio
    conceptkit.sh html  <project>    only the .html twins of the Markdown files
    conceptkit.sh site               the .html twins of every Markdown file under java/beginner
    conceptkit.sh scaffold <module> <project> <package> <MainClass>
                                     a new project folder: Gradle build and wrapper, voice settings

<project> is the project's directory name, for example cia-triad.
Output is one line per step; full tool output goes to <project>/build/conceptkit.log.
Nothing here uploads anything.
"""

import os
import re
import shutil
import subprocess
import sys
from pathlib import Path

from . import generate, publish_docs, site

REPO = Path(__file__).resolve().parents[3]
VIDEOKIT = REPO / "tools" / "videokit" / "videokit.sh"

VOICE_TOML = """# The voice (amy-slow): Piper, open source (MIT), US female,
# slower than normal with longer pauses, for beginners listening without a screen.
[voice]
engine = "piper"
voice  = "en_US-amy-medium"
speed  = 0.8

[pacing]
sentence_gap = 0.7
pause_scale  = 1.5
"""

BUILD_VIDEO = """#!/usr/bin/env bash
# Builds this project's video with the shared videokit library (see video/videokit.toml).
set -euo pipefail
cd "$(dirname "$0")"
exec ../../../../tools/videokit/videokit.sh "${1:-all}" . "${@:2}"
"""


BUILD_GRADLE = """plugins {
    id 'java'
    id 'application'
}

group = 'com.jk.explore.security'
version = '1.0-SNAPSHOT'

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation platform('org.junit:junit-bom:5.10.2')
    testImplementation 'org.junit.jupiter:junit-jupiter'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}

application {
    mainClass = 'com.jk.explore.security.%s.%s'
}

test {
    useJUnitPlatform()
}
"""

SETTINGS_GRADLE = """// Lets Gradle find or download a JDK 25 by itself when none is installed.
plugins {
    id 'org.gradle.toolchains.foojay-resolver-convention' version '1.0.0'
}

rootProject.name = '%s'
"""


def scaffold(module, name, package, main_class):
    """A new project: Gradle build and wrapper (copied from the first project), packages, video settings."""
    p = REPO / "java" / "beginner" / module / name
    if p.exists():
        raise SystemExit("conceptkit: %s already exists" % p)
    template = project_dir("cia-triad")
    (p / "src/main/java/com/jk/explore/security" / package).mkdir(parents=True)
    (p / "src/test/java/com/jk/explore/security" / package).mkdir(parents=True)
    (p / "docs").mkdir()
    for f in ("gradlew", "gradlew.bat"):
        shutil.copy2(template / f, p / f)
    shutil.copytree(template / "gradle", p / "gradle")
    (p / "settings.gradle").write_text(SETTINGS_GRADLE % name)
    (p / "build.gradle").write_text(BUILD_GRADLE % (package, main_class))
    ensure_video_files(p)
    return p


def project_dir(name):
    hits = sorted(REPO.glob("*/beginner/*/%s/concept.toml" % name))
    if len(hits) != 1:
        raise SystemExit("conceptkit: %s project %r" % ("no" if not hits else "more than one", name))
    return hits[0].parent


def java_args():
    """Where Gradle can find JDK 25 on this machine; the toolchain resolver downloads one otherwise."""
    for cand in (os.environ.get("JDK25_HOME"), "/opt/homebrew/opt/openjdk@25", "/opt/homebrew/opt/openjdk"):
        if cand and Path(cand).exists():
            return ["-Dorg.gradle.java.installations.paths=" + cand]
    return []


def run(log, *cmd, cwd=None):
    r = subprocess.run([str(c) for c in cmd], cwd=cwd, capture_output=True, text=True)
    log.write("$ %s\n%s%s\n" % (" ".join(str(c) for c in cmd), r.stdout, r.stderr))
    # onnxruntime (behind the Piper voice) can abort with this message while the process
    # exits, after every file has been written; that is not a failure.
    if r.returncode and "recursive_mutex lock failed" not in r.stderr + r.stdout:
        raise SystemExit("conceptkit: failed: %s\n%s" % (" ".join(str(c) for c in cmd),
                                                        (r.stdout + r.stderr)[-1500:]))
    return r.stdout


def ensure_video_files(p):
    (p / "video").mkdir(exist_ok=True)
    if not (p / "video" / "videokit.toml").exists():
        (p / "video" / "videokit.toml").write_text(VOICE_TOML)
    if not (p / "video" / "build_video.sh").exists():
        (p / "video" / "build_video.sh").write_text(BUILD_VIDEO)
        (p / "video" / "build_video.sh").chmod(0o755)


def count_tests(p):
    """Tests that actually ran, from Gradle's results (a parameterized test counts once per case)."""
    results = list((p / "build" / "test-results").glob("*/*.xml"))
    if results:
        return sum(int(re.search(r'tests="(\d+)"', f.read_text()).group(1)) for f in results)
    n = 0
    for f in (p / "src" / "test").rglob("*.java"):
        n += len(re.findall(r"@(?:Test|ParameterizedTest|RepeatedTest)\b", f.read_text()))
    return n


def scenes_of(p):
    ns = {}
    exec(compile((p / "video" / "scenes.py").read_text(), "scenes.py", "exec"), ns)
    return ns["SCENES"]


def finish_docs(p, d, log):
    """Everything that depends on the tests and, when it exists, the built video."""
    demo_lines = len((p / "docs" / "expected-output.txt").read_text().splitlines())
    publish_docs.spec_doc(d, p, count_tests(p), demo_lines)
    publish_docs.youtube_doc(d, p, scenes_of(p))
    return "spec.md, youtube.md; %d HTML twins" % site.twins(p)


def build(name, media=True):
    p = project_dir(name)
    d = generate.load(p)
    (p / "build").mkdir(exist_ok=True)
    with open(p / "build" / "conceptkit.log", "a") as log:
        ensure_video_files(p)
        print("  docs      " + generate.all_docs(p))
        run(log, p / "gradlew", "-q", "test", *java_args(), cwd=p)
        if "integrationTest" in (p / "build.gradle").read_text():
            run(log, p / "gradlew", "-q", "integrationTest", *java_args(), cwd=p)   # needs a running Docker
        print("  gradle    %d tests pass" % count_tests(p))
        run(log, VIDEOKIT, "slides", name)
        t = publish_docs.thumbnail(p)
        print("  thumbnail " + (str(t.relative_to(p)) if t else "skipped"))
        if media:
            run(log, VIDEOKIT, "all", name)
            print("  video     mp4, m4a and srt built")
        # docs regenerated animation.html without its player, so the player is always put back (clips are cached)
        run(log, VIDEOKIT, "animation", name)
        print("  animation narration clips and player")
        print("  derived   " + finish_docs(p, d, log))


def main(argv):
    if not argv or argv[0] in ("help", "-h"):
        print(__doc__.strip())
        return
    cmd, rest = argv[0], argv[1:]
    if cmd == "docs":
        print(generate.all_docs(project_dir(rest[0])))
    elif cmd == "build":
        build(rest[0], media="--no-media" not in rest)
    elif cmd == "scaffold":
        print(scaffold(*rest))
    elif cmd == "site":
        print("%d HTML twins" % site.tree(REPO / "java" / "beginner"))
    elif cmd == "html":
        print("%d HTML twins" % site.twins(project_dir(rest[0])))
    else:
        raise SystemExit(__doc__)


if __name__ == "__main__":
    main(sys.argv[1:])
