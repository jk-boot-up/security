"""Upload with a quality check: every video is checked as YouTube serves it.

For each project, one at a time and in learning order:

  1. check the local .mp4 (picture and sound) before anything is sent;
  2. upload it Unlisted, with every field filled as usual;
  3. wait for YouTube to finish its HD and 4K versions, download what a viewer
     gets, and check that copy: every slide's text against the slide itself,
     the sound, the length and the custom thumbnail;
  4. pass: make the video Public. Fail: delete it from YouTube, rebuild the
     video with stronger settings, and try again.

At most three tries per video. After the third failure the video is deleted,
marked failed, and the run stops: the next video is not started until this one
passes. Every try is recorded in docs/youtube-quality.json, which feeds
docs/youtube.md.

    ytquality.sh run   <targets>   check-and-upload, e.g. `run cia-triad` or `run beginner`
    ytquality.sh local <project>   only the local checks, nothing is sent
    ytquality.sh check <project>   only check the copy already on YouTube
"""
import datetime
import json
import os
import re
import shutil
import subprocess
import sys
import time

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import ytupload as Y  # noqa: E402

QUALITY = os.path.join(Y.REPO, "docs", "youtube-quality.json")
WORK = os.path.join(Y.PROFILE, "quality")
LOG = os.path.join(Y.PROFILE, "security-quality.log")
STATUS = os.path.join(Y.PROFILE, "security-quality-status.txt")
VENV_BIN = os.path.join(HERE, ".venv", "bin")
MAX_TRIES = 3

# What each try builds with. A failed check is fixed by rebuilding with the next
# row: a lower CRF keeps more detail in the text, a higher audio bitrate keeps
# more of the voice.
FIXES = [
    {"crf": 18, "audio_bitrate": "192k"},
    {"crf": 14, "audio_bitrate": "256k"},
    {"crf": 10, "audio_bitrate": "320k"},
]

# Pass marks. SSIM compares a frame with the slide it shows (1.0 is identical);
# sharpness is the frame's edge strength over the slide's (blur lowers it).
LIMITS = {
    "local": {"ssim": 0.985, "sharpness": 0.93},
    "youtube": {"ssim": 0.95, "sharpness": 0.80},
    "loudness": (-18.0, -14.0),          # integrated LUFS; the build aims at -16
    "true_peak": 0.0,                    # dBTP; above this the sound clips
    "silence": 4.0,                      # seconds; no pause in the narration is longer
    "length": 1.0,                       # seconds the YouTube copy may differ by
    "thumbnail_ssim": 0.80,
}


def log(line):
    os.makedirs(Y.PROFILE, exist_ok=True)
    with open(LOG, "a") as f:
        f.write(time.strftime("%m-%d %H:%M ") + line + "\n")
    print(line, flush=True)


def status(text):
    with open(STATUS, "w") as f:
        f.write(time.strftime("%Y-%m-%d %H:%M ") + text + "\n")


def load():
    return json.load(open(QUALITY)) if os.path.exists(QUALITY) else {}


def save(q):
    with open(QUALITY, "w") as f:
        json.dump(dict(sorted(q.items())), f, indent=1, ensure_ascii=False)
        f.write("\n")


def record(rel, **fields):
    q = load()
    q.setdefault(rel, {"status": "checking", "tries": []}).update(fields)
    save(q)
    return q[rel]


def add_try(rel, entry):
    q = load()
    q.setdefault(rel, {"status": "checking", "tries": []})["tries"].append(entry)
    save(q)


# ---------------------------------------------------------------- measuring

def run(*args, check=True):
    done = subprocess.run([str(a) for a in args], capture_output=True, text=True)
    if check and done.returncode:
        raise RuntimeError("%s failed: %s" % (args[0], done.stderr[-600:]))
    return done


def probe(path):
    out = run("ffprobe", "-v", "error", "-show_entries",
              "stream=codec_type,codec_name,width,height,sample_rate,channels,r_frame_rate"
              ":format=duration,bit_rate", "-of", "json", path).stdout
    return json.loads(out)


def frame(video, t, dst):
    """The frame at t seconds, scaled to 1920x1080 as a 1080p viewer sees it."""
    run("ffmpeg", "-v", "error", "-y", "-ss", "%.3f" % t, "-i", video, "-frames:v", 1,
        "-vf", "scale=1920:1080:flags=lanczos", dst)


def ssim(video, t, slide):
    """Similarity of the brightness (which carries the text) between the frame at
    t seconds and the slide, both at 1080p, compared in the video's own BT.709
    colour space so no colour conversion step skews the score."""
    err = run("ffmpeg", "-v", "info", "-ss", "%.3f" % t, "-i", video, "-i", slide, "-lavfi",
              "[0]scale=1920:1080:flags=lanczos,format=yuv444p[a];"
              "[1]scale=1920:1080:flags=lanczos:out_color_matrix=bt709:out_range=tv,format=yuv444p[b];"
              "[a][b]ssim", "-frames:v", 1, "-f", "null", "-").stderr
    return float(re.search(r"Y:([\d.]+)", err).group(1))


def image_ssim(a, b):
    err = run("ffmpeg", "-v", "info", "-i", a, "-i", b, "-lavfi",
              "[0]format=gray[a];[1]format=gray[b];[a][b]ssim", "-f", "null", "-").stderr
    return float(re.search(r"All:([\d.]+)", err).group(1))


def edge_strength(path):
    from PIL import Image, ImageFilter, ImageStat
    with Image.open(path) as im:
        return ImageStat.Stat(im.convert("L").filter(ImageFilter.FIND_EDGES)).mean[0]


def scenes(project):
    """[(key, start, end)] from the build's timeline.json."""
    durations = json.load(open(os.path.join(project, "video", "build", "timeline.json")))
    out, t = [], 0.0
    for key, d in durations.items():
        out.append((key, t, t + d))
        t += d
    return out

POSTER_SSIM = 0.90


def check_frames(project, video, limits, tag):
    """Every slide's text: a frame from the middle of each scene against the slide."""
    work = os.path.join(WORK, os.path.basename(project), tag)
    os.makedirs(work, exist_ok=True)
    problems, worst = [], {"ssim": 1.0, "sharpness": 9.9}
    for key, start, end in scenes(project):
        ref = os.path.join(work, key + "-slide.png")
        got = os.path.join(work, key + "-frame.png")
        run("ffmpeg", "-v", "error", "-y", "-i",
            os.path.join(project, "video", "build", key + ".png"),
            "-vf", "scale=1920:1080:flags=lanczos", ref)
        t = start + (end - start) * 0.6
        frame(video, t, got)
        s = ssim(video, t, os.path.join(project, "video", "build", key + ".png"))
        sharp = edge_strength(got) / max(edge_strength(ref), 1e-6)
        worst["ssim"], worst["sharpness"] = min(worst["ssim"], s), min(worst["sharpness"], sharp)
        # The cover (grid, glow, photo) compresses worse than text slides; its design is checked again as the thumbnail.
        need = min(limits["ssim"], POSTER_SSIM) if key.endswith("poster") else limits["ssim"]
        if s < need:
            problems.append("%s: text differs from the slide (SSIM %.3f < %.3f)" % (key, s, need))
        if sharp < limits["sharpness"]:
            problems.append("%s: text is blurred (sharpness %.2f < %.2f)" % (key, sharp, limits["sharpness"]))
    return problems, {k: round(v, 4) for k, v in worst.items()}


def check_audio(path):
    """Loudness, peaks, long silences and the stream itself."""
    problems = []
    info = probe(path)
    a = [s for s in info["streams"] if s["codec_type"] == "audio"]
    if not a:
        return ["no audio track"], {}
    a = a[0]
    if int(a.get("channels", 0)) < 1 or int(a.get("sample_rate", 0)) < 44100:
        problems.append("audio is %s Hz, %s channel(s)" % (a.get("sample_rate"), a.get("channels")))
    err = run("ffmpeg", "-v", "info", "-nostats", "-i", path, "-vn", "-af", "ebur128=peak=true",
              "-f", "null", "-").stderr
    summary = err[err.rfind("Summary:"):]
    lufs = float(re.search(r"I:\s+(-?[\d.]+) LUFS", summary).group(1))
    peak = float(re.search(r"Peak:\s+(-?[\d.inf]+) dBFS", summary).group(1))
    lo, hi = LIMITS["loudness"]
    if not lo <= lufs <= hi:
        problems.append("loudness %.1f LUFS, outside %.0f to %.0f" % (lufs, lo, hi))
    if peak > LIMITS["true_peak"]:
        problems.append("peaks at %.1f dBTP: the sound clips" % peak)
    err = run("ffmpeg", "-v", "info", "-nostats", "-i", path, "-vn", "-af",
              "silencedetect=noise=-50dB:d=%s" % LIMITS["silence"], "-f", "null", "-").stderr
    gaps = re.findall(r"silence_start: ([\d.]+)", err)
    if gaps:
        problems.append("%d silence(s) over %ss, first at %ss" % (len(gaps), LIMITS["silence"], gaps[0]))
    return problems, {"lufs": lufs, "true_peak": peak, "silences": len(gaps)}


def check_local(project):
    """Picture and sound of the .mp4 about to be uploaded."""
    s = Y.sheet(project)
    info = probe(s["video"])
    v = [x for x in info["streams"] if x["codec_type"] == "video"][0]
    problems = []
    if (int(v["width"]), int(v["height"])) != (3840, 2160):
        problems.append("video is %sx%s, not 3840x2160" % (v["width"], v["height"]))
    fp, fs = check_frames(project, s["video"], LIMITS["local"], "local")
    ap, am = check_audio(s["video"])
    return problems + fp + ap, dict(fs, **am, size="%sx%s" % (v["width"], v["height"]),
                                    duration=round(float(info["format"]["duration"]), 2))


# ---------------------------------------------------------------- YouTube's copy

def ytdlp(*args):
    return run(os.path.join(VENV_BIN, "yt-dlp"), "--no-warnings", "--no-progress", *args, check=False)


def formats(vid):
    done = ytdlp("-J", "https://www.youtube.com/watch?v=" + vid)
    if done.returncode:
        return None
    return json.loads(done.stdout).get("formats", [])


def wait_processed(vid, hours=4):
    """Wait until YouTube serves the video at 1080p, and 2160p when it gets there.
    Returns the best height available when done waiting."""
    deadline, best, seen_1080 = time.time() + hours * 3600, 0, None
    while time.time() < deadline:
        fs = formats(vid) or []
        best = max([f.get("height") or 0 for f in fs if f.get("vcodec") not in (None, "none")] or [0])
        if best >= 2160:
            return best
        if best >= 1080:
            seen_1080 = seen_1080 or time.time()
            if time.time() - seen_1080 > 3600:     # 4K not coming: check what is there
                return best
        status("waiting for YouTube to process %s (best so far %sp)" % (vid, best))
        time.sleep(300)
    return best


def download(vid, dst_dir):
    """The 1080p picture and the audio a viewer gets, as two files."""
    os.makedirs(dst_dir, exist_ok=True)
    for f in os.listdir(dst_dir):
        os.remove(os.path.join(dst_dir, f))
    url = "https://www.youtube.com/watch?v=" + vid
    # The 1080p stream browsers play (VP9 or AV1); H.264 only when neither exists.
    v = ytdlp("-f", "bv*[height=1080][vcodec^=vp9]/bv*[height=1080][vcodec^=av01]/bv*[height=1080]", "-o",
              os.path.join(dst_dir, "video.%(ext)s"), url)
    a = ytdlp("-f", "ba[ext=m4a]/ba", "-o", os.path.join(dst_dir, "audio.%(ext)s"), url)
    files = {n.split(".")[0]: os.path.join(dst_dir, n) for n in os.listdir(dst_dir)}
    if v.returncode or a.returncode or "video" not in files or "audio" not in files:
        raise RuntimeError("could not download the YouTube copy: %s" % (v.stderr + a.stderr)[-400:])
    return files["video"], files["audio"]


def check_thumbnail(vid, project):
    import urllib.request
    work = os.path.join(WORK, os.path.basename(project))
    got = os.path.join(work, "yt-thumbnail.jpg")
    ref = os.path.join(work, "thumbnail-ref.png")
    for name in ("maxresdefault", "hq720", "sddefault"):
        try:
            urllib.request.urlretrieve("https://i.ytimg.com/vi/%s/%s.jpg" % (vid, name), got)
            break
        except Exception:
            continue
    else:
        return ["the thumbnail could not be fetched"], None
    run("ffmpeg", "-v", "error", "-y", "-i", os.path.join(project, "docs", Y.THUMBNAIL),
        "-vf", "scale=1280:720", ref)
    run("ffmpeg", "-v", "error", "-y", "-i", got, "-vf", "scale=1280:720", got + ".png")
    s = image_ssim(got + ".png", ref)
    if s < LIMITS["thumbnail_ssim"]:
        return ["the thumbnail on YouTube is not the custom one (SSIM %.2f)" % s], round(s, 3)
    return [], round(s, 3)


def check_youtube(project, vid, local_duration):
    best = wait_processed(vid)
    if best < 1080:
        return ["YouTube still has no 1080p version (best %sp)" % best], {"best": best}
    video, audio = download(vid, os.path.join(WORK, os.path.basename(project), "download"))
    fp, fs = check_frames(project, video, LIMITS["youtube"], "youtube")
    ap, am = check_audio(audio)
    problems = fp + ap
    vlen = float(probe(video)["format"]["duration"])
    alen = float(probe(audio)["format"]["duration"])
    for name, n in (("picture", vlen), ("sound", alen)):
        if abs(n - local_duration) > LIMITS["length"]:
            problems.append("YouTube's %s is %.1fs long, the video is %.1fs" % (name, n, local_duration))
    tp, ts = check_thumbnail(vid, project)
    if best < 2160:
        problems.append("YouTube made no 4K version (best %sp), so text gets a low bitrate" % best)
    return problems + tp, dict(fs, **am, best=best, thumbnail_ssim=ts)


# ---------------------------------------------------------------- fixing

def rebuild(project, fix):
    """Rebuild only the video, with the given settings. The narration is cached."""
    env = dict(os.environ, VIDEOKIT_CRF=str(fix["crf"]), VIDEOKIT_AUDIO_BITRATE=fix["audio_bitrate"])
    done = subprocess.run([os.path.join(Y.REPO, "tools", "videokit", "videokit.sh"), "all", project],
                          env=env, capture_output=True, text=True)
    if done.returncode:
        raise RuntimeError("rebuild failed: " + (done.stdout + done.stderr)[-600:])


def built_with(project):
    """The settings the current .mp4 was built with, if this script built it and
    no slide has been redrawn since."""
    build = os.path.join(project, "video", "build")
    path = os.path.join(build, "quality-settings.json")
    mp4 = Y.sheet(project)["video"]
    if not os.path.exists(path) or not os.path.exists(mp4):
        return None
    slides = [os.path.join(build, n) for n in os.listdir(build) if n.endswith(".png")]
    if any(os.path.getmtime(p) > os.path.getmtime(mp4) for p in slides):
        return None
    return json.load(open(path))


def mark_built(project, fix):
    with open(os.path.join(project, "video", "build", "quality-settings.json"), "w") as f:
        json.dump(fix, f)


# ---------------------------------------------------------------- one video

def one(rel):
    """Check-and-upload one project. Returns True when it is Public and passed."""
    project = os.path.join(Y.JAVA, rel)
    q = load().get(rel, {})
    if q.get("status") == "passed":
        return True
    left = Y.load_registry().get(rel, {})
    if left.get("visibility") == "Public" and left.get("id"):
        return True                                  # already public from an earlier run
    if left.get("visibility") == "Unlisted" and left.get("id"):
        delete(rel, left["id"])                      # an unchecked copy left by a crash
    done_tries = len(q.get("tries", [])) if q.get("status") == "checking" else 0
    if not done_tries:
        record(rel, status="checking", tries=[], started=datetime.datetime.now().isoformat(timespec="minutes"))
    for n in range(done_tries + 1, MAX_TRIES + 1):
        fix = FIXES[n - 1]
        entry = {"try": n, "settings": fix, "at": datetime.datetime.now().isoformat(timespec="minutes")}
        status("%s: try %d of %d, building" % (rel, n, MAX_TRIES))
        if built_with(project) != fix:
            rebuild(project, fix)
            mark_built(project, fix)

        problems, local = check_local(project)
        entry["local"] = local
        if problems:
            entry.update(result="failed locally", problems=problems)
            add_try(rel, entry)
            log("%s try %d: failed locally: %s" % (rel, n, "; ".join(problems)))
            continue

        status("%s: try %d of %d, uploading" % (rel, n, MAX_TRIES))
        vid = upload(rel)
        entry["id"] = vid
        status("%s: try %d of %d, checking YouTube's copy of %s" % (rel, n, MAX_TRIES, vid))
        problems, yt = check_youtube(project, vid, local["duration"])
        entry["youtube"] = yt
        if not problems:
            make_public(rel, vid)
            entry["result"] = "passed"
            add_try(rel, entry)
            record(rel, status="passed", id=vid, finished=datetime.datetime.now().isoformat(timespec="minutes"))
            log("%s try %d: PASSED, public at https://youtu.be/%s" % (rel, n, vid))
            Y.regenerate()
            return True
        entry.update(result="failed on YouTube", problems=problems)
        add_try(rel, entry)
        log("%s try %d: failed on YouTube: %s" % (rel, n, "; ".join(problems)))
        delete(rel, vid)

    record(rel, status="failed", finished=datetime.datetime.now().isoformat(timespec="minutes"))
    log("%s: FAILED after %d tries; deleted from YouTube" % (rel, MAX_TRIES))
    Y.regenerate()
    return False


def upload(rel):
    """Upload Unlisted; record it so a crash never leaves an unknown copy behind."""
    def remember(vid, warn=()):
        reg = Y.load_registry()
        reg[rel] = {"title": Y.sheet(os.path.join(Y.JAVA, rel))["title"], "id": vid,
                    "visibility": "Unlisted", "module": rel.split("/")[1],
                    "recorded": datetime.date.today().isoformat()}
        if warn:
            reg[rel]["problem"] = "; ".join(warn)
        Y.save_registry(reg)

    Y.DRAFT["link"] = None
    pw, page = Y.attach()
    try:
        try:
            link, warn = Y.upload_one(page, rel, visibility="UNLISTED")
        except BaseException:
            # The file may already be on YouTube as a draft: record it, so the retry deletes it.
            try:
                link = Y.DRAFT["link"] or Y.published_link(page)
            except Exception:
                link = Y.DRAFT["link"]
            m = re.search(r"youtu\.be/([\w-]+)", link or "")
            if m:
                try:
                    Y.wait_uploaded(page, minutes=20)     # deleting needs the upload finished
                except Exception:
                    pass
                remember(m.group(1))
            raise
    finally:
        pw.stop()
    m = re.search(r"youtu\.be/([\w-]+)", link or "")
    if not m:
        raise SystemExit("%s: uploaded, but no link was shown; check Studio by hand" % rel)
    remember(m.group(1), warn)
    return m.group(1)


def make_public(rel, vid):
    pw, page = Y.attach()
    try:
        Y.set_visibility(page, vid, "PUBLIC")
    finally:
        pw.stop()
    reg = Y.load_registry()
    reg[rel]["visibility"] = "Public"
    Y.save_registry(reg)


def delete(rel, vid):
    """Delete a copy this script uploaded, and forget it."""
    reg = Y.load_registry()
    if reg.get(rel, {}).get("id") != vid:
        raise SystemExit("refusing to delete %s: not the copy recorded for %s" % (vid, rel))
    pw, page = Y.attach()
    try:
        Y.delete_video(page, vid)
    finally:
        pw.stop()
    del reg[rel]
    Y.save_registry(reg)
    log("%s: deleted %s from YouTube" % (rel, vid))


def run_all(targets):
    todo = Y.expand(targets)
    review_path = os.path.join(Y.REPO, "docs", "youtube-review.json")
    for rel in todo:
        if load().get(rel, {}).get("status") == "passed":
            continue
        reviewed = json.load(open(review_path)) if os.path.exists(review_path) else {}
        if rel not in reviewed:                      # never upload before review; never skip ahead
            status("PAUSED: %s is not reviewed yet (docs/youtube-review.json); later videos wait" % rel)
            log("%s: not reviewed yet; stopping here to keep the playlist order" % rel)
            return
        if Y.thumbnail_limited():
            status("STOPPED: YouTube's daily custom-thumbnail limit; run again later")
            return
        crashes = 0
        while True:
            try:
                ok = one(rel)
                break
            except Y.DailyLimit:
                status("waiting 6h: YouTube's daily upload limit")
                log("%s: daily upload limit; waiting 6 hours" % rel)
                time.sleep(6 * 3600)
            except (Exception, SystemExit) as e:   # a Studio or network hiccup: retry this video
                crashes += 1
                why = str(e).strip().splitlines()[0][:300] if str(e).strip() else type(e).__name__
                log("%s: interrupted (%d of 3): %s" % (rel, crashes, why))
                if crashes >= 3:
                    status("STOPPED: %s interrupted 3 times; last: %s" % (rel, why))
                    return
                if Y.thumbnail_limited():
                    status("waiting 2h: YouTube's custom-thumbnail limit")
                    time.sleep(2 * 3600)
                else:
                    time.sleep(600)
        if not ok:
            status("STOPPED: %s failed three tries; see docs/youtube-quality.json" % rel)
            return
    q = load()
    passed = sum(1 for r in todo if q.get(r, {}).get("status") == "passed")
    status("FINISHED: %d of %d passed" % (passed, len(todo)))


def main():
    cmd, args = (sys.argv[1] if len(sys.argv) > 1 else "help"), sys.argv[2:]
    if cmd == "run" and args:
        run_all(args)
    elif cmd == "local" and args:
        for rel in Y.expand(args):
            problems, m = check_local(os.path.join(Y.JAVA, rel))
            print(rel, json.dumps(m), "\n  " + ("\n  ".join(problems) if problems else "PASS"))
    elif cmd == "check" and args:
        for rel in Y.expand(args):
            project = os.path.join(Y.JAVA, rel)
            vid = Y.load_registry()[rel]["id"]
            problems, m = check_youtube(project, vid, float(probe(Y.sheet(project)["video"])["format"]["duration"]))
            print(rel, vid, json.dumps(m), "\n  " + ("\n  ".join(problems) if problems else "PASS"))
    else:
        print(__doc__.strip())


if __name__ == "__main__":
    main()
