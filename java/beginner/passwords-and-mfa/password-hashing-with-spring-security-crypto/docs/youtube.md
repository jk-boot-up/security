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
00:52 The Scenario
01:15 The Words
01:42 What the Letters Stand For
02:24 What bcrypt stores for welcome1
03:20 Two weak ways to store welcome1
04:14 Act One — Two weak encoders, written out here
04:30 How a login is checked, step by step
05:27 Act Two — One call: the delegating encoder
05:54 Act Two — One call: the delegating encoder
06:16 Four algorithms, one interface
07:04 Act Three — Four algorithms, one interface
07:12 The same attack, at two costs
07:59 Act Four — The same attack, at two costs
08:12 An upgrade at login
09:12 Act Five — Moving to a stronger algorithm without asking anyone
09:24 The bill: a planted value, and memory
10:17 Act Six — The bill: an open list, and memory
10:26 How To Use It
10:53 The Java Classes
11:02 SpringHashingDemo — the default, and an allow-list
11:22 BcryptSteps — the three steps of a login
11:42 RepeatableSalts — the same salts on every run
11:55 Attacker — one guess at a time
12:07 FastSaltedSha256Encoder — salt, then one quick hash
12:20 PlainTextEncoder — no hashing at all
12:28 Customers — the passwords and the guesses
12:35 Thanks for Watching
```

## Tags

spring security, password encoder, bcrypt, argon2, scrypt, password hashing, java, cybersecurity for beginners, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/password-hashing-with-spring-security-crypto-explained.mp4` |
| Captions | `video/password-hashing-with-spring-security-crypto-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Passwords and MFA in Java - With Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (44)
- [ ] tags are at most 500 characters in total — ok (154)
- [ ] description is at most 5,000 characters — ok (670)
- [ ] chapters start at 00:00 and there are at least three
