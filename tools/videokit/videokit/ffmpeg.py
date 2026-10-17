"""Thin wrappers over the ffmpeg and ffprobe command lines."""

import subprocess


def run(*args):
    """Run ffmpeg quietly; raise with its error text if it fails."""
    cmd = ["ffmpeg", "-y", "-hide_banner", "-loglevel", "error", *map(str, args)]
    done = subprocess.run(cmd, capture_output=True, text=True)
    if done.returncode:
        raise RuntimeError("ffmpeg failed: %s\n%s" % (" ".join(cmd), done.stderr.strip()))


def stderr(*args):
    """Run ffmpeg and return what it printed, for filters that report there."""
    cmd = ["ffmpeg", "-hide_banner", "-nostats", *map(str, args)]
    return subprocess.run(cmd, capture_output=True, text=True).stderr


def duration(path):
    """Container duration in seconds."""
    out = subprocess.check_output([
        "ffprobe", "-v", "error", "-show_entries", "format=duration",
        "-of", "default=nw=1:nk=1", str(path)])
    return float(out.strip())


def packet_times(path, stream="a"):
    """Presentation time of every packet in the first stream of a kind."""
    out = subprocess.check_output([
        "ffprobe", "-v", "error", "-select_streams", stream,
        "-show_entries", "packet=pts_time", "-of", "csv=p=0", str(path)]).decode()
    return [float(x.rstrip(",")) for x in out.split() if x.strip()]
