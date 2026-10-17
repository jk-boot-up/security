"""Narration for a project's docs/animation.html.

    videokit.sh animation <project>

1. Reads the spoken text of every step out of the page's STEPS array.
2. Voices each step in the project's voice into docs/audio/step-N.m4a
   (git-ignored, cached by text, never embedded in the page).
3. Injects one small script between videokit markers that adds a
   Narration on/off switch and a play/pause button for the current step.
"""

import hashlib
import json
import re
import subprocess
import tempfile
from pathlib import Path

import soundfile as sf

from . import audio, narrate, tts

WEB = Path(__file__).parent / "web"
BEGIN, END = "<!-- videokit:narration -->", "<!-- /videokit:narration -->"
BLOCK = re.compile(re.escape(BEGIN) + r".*?" + re.escape(END) + r"\n?", re.S)


def steps(html_path):
    out = subprocess.run(["node", str(WEB / "extract_steps.js"), str(html_path)],
                         check=True, capture_output=True, text=True).stdout
    return json.loads(out)


HEX = re.compile(r"#([0-9a-fA-F]{6})\b")
VAR = re.compile(r"var\(--vk-([0-9a-f]{6}), #\1\)")


def themeable(html):
    """Turn every six-digit colour in the page's CSS into a --vk-* variable
    that falls back to the colour itself, so ux.js can switch themes by
    remapping the palette. Running it twice changes nothing."""
    def colours(text):
        text = VAR.sub(lambda m: "#" + m.group(1), text)
        return HEX.sub(lambda m: "var(--vk-%s, #%s)" % ((m.group(1).lower(),) * 2), text)
    html = re.sub(r"(<style[^>]*>)(.*?)(</style>)",
                  lambda m: m.group(1) + colours(m.group(2)) + m.group(3), html, flags=re.S | re.I)
    return re.sub(r'(\sstyle=")([^"]*)(")',
                  lambda m: m.group(1) + colours(m.group(2)) + m.group(3), html)


def inject(html, texts, audio_dir="audio"):
    js = (WEB / "player.js").read_text().replace("__VK_STEPS__", json.dumps(texts)) \
        .replace("__VK_AUDIO_DIR__", audio_dir)
    ux = (WEB / "ux.js").read_text()
    block = "%s\n<script>\n%s</script>\n<script>\n%s</script>\n%s\n" % (BEGIN, js, ux, END)
    html = themeable(BLOCK.sub("", html))
    if audio_dir != "audio":
        # A page that plays its own clips names "audio/step-N.m4a"; a version's
        # copy must play the version's clips, or it speaks in the main voice.
        html = re.sub(r"""(["'`])audio/""", lambda m: m.group(1) + audio_dir + "/", html)
    i = html.lower().rfind("</body>")
    return html[:i] + block + html[i:] if i >= 0 else html + block


def voice(texts, settings, out_dir, force=False):
    """step-N.m4a for every step whose text changed; returns how many were voiced."""
    out_dir.mkdir(parents=True, exist_ok=True)
    index = out_dir / ".cache.json"
    cache = json.loads(index.read_text()) if index.exists() else {}
    engine, voiced = None, 0
    for n, text in enumerate(texts, 1):
        name = "step-%d.m4a" % n
        key = hashlib.sha1((settings.voice_id() + "\n" + text).encode()).hexdigest()[:16]
        if not text or (not force and cache.get(name) == key and (out_dir / name).exists()):
            continue
        if narrate.FORBIDDEN.search(text):
            raise SystemExit("animation step %d claims to be the author; fix the page" % n)
        engine = engine or tts.from_settings(settings)
        samples, _ = narrate.speak(engine, text, settings.sentence_gap, settings.pause_scale)
        with tempfile.TemporaryDirectory() as tmp:
            wav = Path(tmp) / "s.wav"
            sf.write(wav, samples, engine.sample_rate)
            audio.encode(wav, out_dir / name,
                         audio.loudnorm_filter(wav, settings.loudness, settings.true_peak), "128k")
        cache[name] = key
        voiced += 1
    for old in out_dir.glob("step-*.m4a"):                    # steps that were removed
        if int(old.stem.split("-")[1]) > len(texts):
            old.unlink()
            cache.pop(old.name, None)
    index.write_text(json.dumps(cache, indent=1))
    return voiced


def build(project, force=False):
    page = project.dir / "docs" / "animation.html"
    if not page.exists():
        return "no animation.html"
    texts = steps(page)
    if not any(texts):
        raise SystemExit("no step narration found in %s" % page)
    settings = project.settings()
    if settings.version:
        # A version gets its own clips and its own copy of the page; the
        # published animation.html and docs/audio/ are left untouched.
        audio_dir = "audio-" + settings.version
        n = voice(texts, settings, page.parent / audio_dir, force)
        out = page.with_name("animation-%s.html" % settings.version)
        out.write_text(inject(page.read_text(), texts, audio_dir))
        return "%d steps, %d voiced -> %s" % (len(texts), n, out.name)
    # Every animation speaks in the project's own voice (amy-slow by default).
    audio_dir = "audio"
    n = voice(texts, settings, page.parent / audio_dir, force)
    html = page.read_text()
    new = inject(html, texts, audio_dir)
    if new != html:
        page.write_text(new)
    return "%d steps, %d voiced" % (len(texts), n)
