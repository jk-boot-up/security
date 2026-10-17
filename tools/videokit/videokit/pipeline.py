"""The build stages, composed from the modules. Each stage can run alone.

    slides     project make_slides.py, or the standard kinds -> build/<key>.png
    narrate    scenes.py narration -> build/tts/<key>.wav + .json (cached)
    video      slides + narration -> the .mp4, .m4a and poster.png
    subtitles  phrase timings -> the .srt

Stages report one line each, and full tool output goes to build/videokit.log,
so a whole rebuild costs a handful of lines to read.
"""

import json
import shutil
import time

from . import audio, narrate, slides, subtitles, video

ORDER = ("slides", "narrate", "video", "subtitles")
BUILD_STAGES = ORDER


def stage_slides(project, settings, opts):
    paths = slides.render(project)
    return "%d slides" % len(paths)


def stage_narrate(project, settings, opts):
    n = narrate.narrate_scenes(project.scenes, settings, project.build / "tts", opts.get("force"))
    return "%d of %d scenes voiced (%s %s), rest cached" % (
        n, len(project.scenes), settings.engine, settings.voice)


def stage_video(project, settings, opts):
    b, tts_dir = project.build, project.build / "tts"
    clips, wavs, durations = [], [], {}
    for scene in project.scenes:
        key = scene["key"]
        wav = b / ("%s.wav" % key)
        frames = audio.prepare_scene(tts_dir / (key + ".wav"), wav, settings.tail_pad, settings.fps)
        clip = b / ("scene-%s.mp4" % key)
        video.slide_clip(b / (key + ".png"), frames, settings.fps, clip)
        clips.append(clip.name), wavs.append(wav.name)
        durations[key] = frames / settings.fps

    video.concat(clips, b / "joined.mp4", b / "concat.txt")
    audio.concat(wavs, b / "narration.wav", b / "concat-audio.txt")
    loud = audio.loudnorm_filter(b / "narration.wav", settings.loudness,
                                 settings.true_peak, settings.loudness_range)
    video.mux(b / "joined.mp4", b / "narration.wav", project.mp4, loud, settings.audio_bitrate)
    video.extract_audio(project.mp4, project.m4a)
    if not settings.version:          # a version never replaces the published poster
        video.copy_poster(b / (project.scenes[0]["key"] + ".png"), project.poster)
    (b / "timeline.json").write_text(json.dumps(durations, indent=1))

    gaps = video.audio_gaps(project.mp4)
    if gaps:
        raise RuntimeError("audio timeline has %d gap(s), first at %.3fs" % (len(gaps), gaps[0][0]))
    if not opts.get("keep"):
        for name in clips + wavs + ["joined.mp4", "narration.wav", "concat.txt", "concat-audio.txt"]:
            (b / name).unlink(missing_ok=True)
    total = sum(durations.values())
    return "%s  %d:%02d, audio continuous" % (project.mp4.name, total // 60, total % 60)


def stage_subtitles(project, settings, opts):
    durations = json.loads((project.build / "timeline.json").read_text())
    timings = {s["key"]: narrate.timings(project.build / "tts", s["key"]) for s in project.scenes}
    srt = subtitles.build(project.scenes, durations, timings, project.split_cues(),
                          settings.tail_pad)
    project.srt.write_text(srt)
    return "%s  %d cues" % (project.srt.name, srt.count(" --> "))


STAGES = dict(slides=stage_slides, narrate=stage_narrate, video=stage_video,
              subtitles=stage_subtitles)


def run(project, stages, **opts):
    """Run the named stages in pipeline order, one summary line each."""
    settings = project.settings()
    project.build.mkdir(parents=True, exist_ok=True)
    with open(project.build / "videokit.log", "a") as log:
        opts["log"] = log
        for name in (s for s in ORDER if s in stages):
            t = time.time()
            try:
                msg = STAGES[name](project, settings, opts)
            except Exception as e:
                log.write("FAILED %s: %s\n" % (name, e))
                raise SystemExit("videokit: %s failed: %s" % (name, e))
            print("  %-9s %s  (%.0fs)" % (name, msg, time.time() - t))


def clean(project):
    """Forget everything generated, including the narration cache."""
    shutil.rmtree(project.build, ignore_errors=True)
