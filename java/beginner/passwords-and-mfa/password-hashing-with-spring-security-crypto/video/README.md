# Password Hashing with Spring Security Crypto — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `password-hashing-with-spring-security-crypto-explained.mp4` | the video, 1920×1080 |
| `password-hashing-with-spring-security-crypto-explained.m4a` | audio only |
| `password-hashing-with-spring-security-crypto-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> Password hashing with Spring Security Crypto, in Java: the library's password encoders turn a password into a slow, salted hash in one call, tag the stored text with the algorithm's name, and offer bcrypt, PBKDF2, scrypt and Argon2 behind one interface. Explained with an online store's customer accounts, like a hotel that replaces its door locks guest by guest, without closing the hotel. We see the two deprecated encoders, the delegating encoder, four algorithms side by side, the same attack at two costs, a migration from bcrypt to Argon2 at login, and finish with the bill: an open algorithm list lets someone plant a plain-text password, and Argon2 costs memory.
