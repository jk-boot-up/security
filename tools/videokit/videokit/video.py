"""Video steps: slide clips, joining, muxing, and an integrity check."""

import os
import shutil

from . import ffmpeg


def slide_clip(png, frames, fps, dst):
    """A still slide held for exactly `frames` frames, video only.

    -bf 0: B-frames save nothing on a still image and push the first video
    timestamp past the audio's, which shows black at 0:00.
    """
    ffmpeg.run("-loop", 1, "-i", png, "-frames:v", frames, "-c:v", "libx264",
               "-preset", "slow", "-crf", 18, "-pix_fmt", "yuv420p", "-r", fps,
               "-bf", 0, "-an", dst)


def concat(paths, dst, list_file):
    """Join clips that share one encoding, without re-encoding."""
    with open(list_file, "w") as fh:
        fh.writelines("file '%s'\n" % p for p in paths)
    ffmpeg.run("-f", "concat", "-safe", "0", "-i", list_file, "-c", "copy", dst)


def mux(video, narration_wav, dst, audio_filter, bitrate="192k"):
    """Attach the narration, encoded to AAC exactly once, to the video.

    The result is written via an intermediate and finished with:
      -ignore_editlist  drops the AAC priming edit list, which otherwise
                        starts the video 21 ms late and shows black at 0:00;
      +faststart        puts the index first so players can start at once.
    """
    tmp = str(dst) + ".muxing.mp4"
    ffmpeg.run("-i", video, "-i", narration_wav, "-map", "0:v", "-map", "1:a",
               "-c:v", "copy", "-af", audio_filter, "-c:a", "aac", "-b:a", bitrate,
               "-ar", 48000, "-ac", 2, tmp)
    ffmpeg.run("-ignore_editlist", 1, "-i", tmp, "-c", "copy", "-movflags", "+faststart", dst)
    os.remove(tmp)


def extract_audio(mp4, dst):
    """The audio-only edition, copied out of the finished video."""
    ffmpeg.run("-i", mp4, "-vn", "-c:a", "copy", "-movflags", "+faststart", dst)


def audio_gaps(mp4, packet=1024 / 48000):
    """Any holes in the audio timeline, as (time, gap) pairs. Should be none."""
    pts = ffmpeg.packet_times(mp4, "a")
    return [(pts[i - 1], pts[i] - pts[i - 1]) for i in range(1, len(pts))
            if pts[i] - pts[i - 1] > packet * 1.5]


def copy_poster(first_slide, dst):
    shutil.copyfile(first_slide, dst)
