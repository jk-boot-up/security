# YouTube sheet — Security Misconfiguration

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Security Misconfiguration in Java (OWASP A02: Defaults, Debug Pages, Drift)

## Description

Security misconfiguration in Java, OWASP Top 10 A02: the settings nobody changed. Explained with an online store, like a shop that fits a strong lock and leaves the spare key in the door. We start the store with its shipped defaults and let a checker find a default admin password, a debug page that prints the secret, a directory listing that shows a backup file, and stack traces in error pages; harden it with secure defaults and a gate that refuses to start with a default secret; catch development settings that drift into production; remove version banners, the TRACE method and a settings file anyone can read; and finish with the bill: a baseline that must be checked on every change, at every startup, and against the live store.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/intermediate/secure-configuration/security-misconfiguration

Chapters:

```
00:00 Introduction
01:10 The Scenario
01:30 The Words
01:57 Act One — The store started with its shipped defaults
02:33 Act Two — Secure defaults, and a gate at startup
03:06 Act Three — Drift: development settings that reach production
03:42 Act Four — Banners, methods and file permissions
04:20 Act Five — The bill: a baseline that must be checked again and again
04:57 Where Each Check Runs
05:25 Who Does What
05:54 Where You Have Seen It
06:17 How To Use It
06:43 Thanks for Watching
```

## Tags

security misconfiguration, owasp top 10, a02, secure defaults, hardening, debug endpoint, directory listing, java, application security

## Files

| What | File |
| --- | --- |
| Video | `video/security-misconfiguration-explained.mp4` |
| Captions | `video/security-misconfiguration-explained.srt` |
| Thumbnail | `docs/thumbnail.png` (1280×720) |

## Settings

Public with instant Premiere; not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Security Foundations in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (75)
- [ ] tags are at most 500 characters in total — ok (127)
- [ ] description is at most 5,000 characters — ok (738)
- [ ] chapters start at 00:00 and there are at least three
