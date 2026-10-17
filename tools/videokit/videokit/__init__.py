"""videokit -- the shared audio/video pipeline for the concept projects.

Every module is usable on its own; `pipeline` composes them into stages and
`__main__` exposes those stages on the command line.

    config      Settings: voice, pacing and output parameters, per project
    project     Project: where a project's scenes, slides and outputs live
    script      narration markup -> phrases, spoken text, caption cues
    tts         pluggable voice engines (kokoro, say)
    narrate     phrases -> speech with real pauses, plus phrase timings, cached
    audio       trimming, clean-up, frame alignment, joining, loudness
    video       slide clips, concatenation, muxing, integrity checks
    subtitles   SRT timed from the phrase timings
    slides      runs a project's own make_slides.py
    publish     runs the shared docs generators (narration, YouTube doc, spec)
    samples     renders one text through several voices, for auditions
    pipeline    the stages, composed from the above
"""
