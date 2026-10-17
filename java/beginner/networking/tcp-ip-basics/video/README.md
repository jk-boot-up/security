# TCP/IP Basics for Security — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `tcp-ip-basics-explained.mp4` | the video, 1920×1080 |
| `tcp-ip-basics-explained.m4a` | audio only |
| `tcp-ip-basics-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> TCP/IP basics for security in Java: packets, checksums, the three-way handshake, SYN floods and guessable sequence numbers. Explained with an online store. We build an IPv4 packet and a TCP segment byte by byte, see that a checksum catches accidents but not attackers, run the three-way handshake as a state machine, fill its table with a SYN flood and defend with SYN cookies, forge a connection by guessing sequence numbers, and watch a real connection carry a card number in the clear.
