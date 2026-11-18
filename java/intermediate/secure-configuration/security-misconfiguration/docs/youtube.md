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
01:17 The Scenario
01:37 The Words
02:04 What the Letters Stand For
03:13 One store, two settings files
04:09 Four requests a scanner tries first
05:08 Act One — The store started with its shipped defaults
05:35 Act One — The store started with its shipped defaults
05:53 Act One — The store started with its shipped defaults
06:08 Eight checks, and a gate before the port opens
07:03 Act Two — Secure defaults, and a gate at startup
07:26 Act Two — Secure defaults, and a gate at startup
07:48 Act Two — Secure defaults, and a gate at startup
08:06 A production file copied from development
08:54 Act Three — Drift: development settings that reach production
09:14 Act Three — Drift: development settings that reach production
09:32 Three small leaks
10:33 Act Four — Banners, methods and file permissions
10:56 Act Four — Banners, methods and file permissions
11:13 A new setting, and where each check runs
12:02 Act Five — The bill: a baseline that must be checked again and again
12:30 Where You Have Seen It
12:53 How To Use It
13:19 The Java Classes
13:35 StoreServer — a debug page and detailed errors, behind a switch
13:50 ConfigChecker — one check: send a request, judge the reply
14:03 StartupGate — runs before the port opens
14:17 Secrets — empty, a known default, or too short
14:28 ProfileDrift — three kinds of drift
14:43 ConfigFile — explicit permissions
14:55 Thanks for Watching
```

## Tags

security misconfiguration, owasp top 10, a02, secure defaults, hardening, debug endpoint, directory listing, java, application security, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/security-misconfiguration-explained.mp4` |
| Captions | `video/security-misconfiguration-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Secure Configuration in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (75)
- [ ] tags are at most 500 characters in total — ok (178)
- [ ] description is at most 5,000 characters — ok (738)
- [ ] chapters start at 00:00 and there are at least three
