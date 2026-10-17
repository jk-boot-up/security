# YouTube sheet — Password Hashing with Spring Security Crypto

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Password Hashing with Spring Security Crypto

## Description

Password hashing with Spring Security Crypto, in Java: the library's password encoders turn a password into a slow, salted hash in one call, tag the stored text with the algorithm's name, and offer bcrypt, PBKDF2, scrypt and Argon2 behind one interface. Explained with an online store's customer accounts, like a hotel that replaces its door locks guest by guest, without closing the hotel. We see the two deprecated encoders, the delegating encoder, four algorithms side by side, the same attack at two costs, a migration from bcrypt to Argon2 at login, and finish with the bill: an open algorithm list lets someone plant a plain-text password, and Argon2 costs memory.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/beginner/passwords-and-mfa/password-hashing-with-spring-security-crypto

Chapters:

```
00:00 Introduction
01:19 The Scenario
01:49 The Interface
02:20 Act One — Two the library marks as deprecated
03:06 Act Two — One call: the delegating encoder
03:55 Act Three — Four algorithms, one interface
04:46 Act Four — The same attack, at two costs
05:35 Act Five — Moving to a stronger algorithm without asking anyone
06:28 Act Six — The bill: an open list, and memory
07:20 Plain Version, Library Version
07:54 Who Does What
08:25 Where You Have Seen It
09:03 How To Use It
09:30 Thanks for Watching
```

## Tags

spring security, password encoder, bcrypt, argon2, scrypt, password hashing, java, cybersecurity for beginners

## Files

| What | File |
| --- | --- |
| Video | `video/password-hashing-with-spring-security-crypto-explained.mp4` |
| Captions | `video/password-hashing-with-spring-security-crypto-explained.srt` |
| Thumbnail | `docs/thumbnail.png` (1280×720) |

## Settings

Public with instant Premiere; not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Security Foundations in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (44)
- [ ] tags are at most 500 characters in total — ok (103)
- [ ] description is at most 5,000 characters — ok (670)
- [ ] chapters start at 00:00 and there are at least three
