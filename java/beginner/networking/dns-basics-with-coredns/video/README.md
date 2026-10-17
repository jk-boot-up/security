# DNS Basics with CoreDNS — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `dns-basics-with-coredns-explained.mp4` | the video, 1920×1080 |
| `dns-basics-with-coredns-explained.m4a` | audio only |
| `dns-basics-with-coredns-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> DNS basics with CoreDNS, in Java: run a real DNS server in a container and ask it questions with our own code. Explained with an online store, like a phone book that prints whatever the last editor wrote. We query the server over UDP and TCP, see a name that does not exist, a zone it refuses to answer, a rewrite rule, the cache and the countdown of the time to live, and a zone with a wrong record that the server serves exactly like the right one, and finish with what really protects names: control of the zone file and DNSSEC.
