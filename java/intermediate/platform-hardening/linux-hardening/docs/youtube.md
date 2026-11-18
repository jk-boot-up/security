# YouTube sheet — Linux Hardening Checker

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Linux Hardening Checker in Java: Permissions, SUID, Accounts, SSH, Ports

## Description

A Linux hardening checker in Java for an online store's server image: unexpected set-user-id files, loose permissions on secrets, a second root account and empty passwords, SSH root login and password authentication, kernel settings, and services listening on every interface; it applies the permission fixes a machine can make safely, leaves the decisions to a person, and runs again in every build.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/intermediate/platform-hardening/linux-hardening

Chapters:

```
00:00 Introduction
00:57 The Scenario
01:30 The Words
02:14 What the Letters Stand For
03:23 What the Letters Stand For (continued)
04:36 What the Letters Stand For (continued)
04:45 How a file mode is read
05:49 Twenty findings, and the seven a machine can fix
06:44 Act One — The shop's server image, before any fix
07:16 Act One — The shop's server image, before any fix
07:36 Act One — The shop's server image, before any fix
07:57 How each new mode is made
09:00 Act Two — The fixes a machine can make
09:19 Act Two — The fixes a machine can make
09:32 Reading an account line, and the port table in hex
10:41 Act Three — The decisions a person makes
10:58 Act Three — The decisions a person makes
11:13 Act Three — The decisions a person makes
11:33 A later change fails the build
12:19 Act Four — Running it again, in every build
12:46 What It Costs
13:20 Where You Have Seen It
13:58 How To Use It
14:29 The Java Classes
14:40 Modes — the bits, and a digit as letters
15:00 Scanner — an unexpected set-user-id file
15:17 Scanner — take bits away, or keep only the allowed ones
15:36 HostFiles — an account that can log in
15:53 PortTable — hex, and the address backwards
16:07 BuildGate — fail at a severity
16:18 Thanks for Watching
```

## Tags

linux hardening, suid, file permissions, sshd_config, cis benchmark, java, application security, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/linux-hardening-explained.mp4` |
| Captions | `video/linux-hardening-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Platform Hardening in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (72)
- [ ] tags are at most 500 characters in total — ok (140)
- [ ] description is at most 5,000 characters — ok (400)
- [ ] chapters start at 00:00 and there are at least three
