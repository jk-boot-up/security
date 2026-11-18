# YouTube sheet — Linux Hardening with Docker

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Linux Hardening with Docker: Scanning a Real Container and Fixing the Image (CIS Benchmarks)

## Description

Two real Docker images built from Dockerfiles for an online store's server: a typical one with an account without a password, a second root, a copied shell with the setuid bit, a world-writable upload folder, loose modes on the shadow file, a host key and a config, an sshd_config that allows root and empty passwords, and three listeners on every interface; a scan run as root inside the container finds 19 findings, a machine fixes 7 and docker diff shows where they live, a new container has all 19 again, and the same shop with its decisions in the recipe has none.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/intermediate/platform-hardening/linux-hardening-with-docker

Chapters:

```
00:00 Introduction
00:50 The Scenario
01:21 The Words
01:56 What the Letters Stand For
03:01 What the Letters Stand For (continued)
03:43 Where a fix in a container lives
04:32 Nineteen findings in a real container
05:26 Act One — What the scan finds in a typical image
05:48 Act One — What the scan finds in a typical image
06:11 Act One — What the scan finds in a typical image
06:21 Act One — What the scan finds in a typical image
06:35 Nineteen, twelve, and nineteen again
07:19 Act Two — The fixes a machine can make, and where they live
07:34 Act Two — The fixes a machine can make, and where they live
07:50 The same script, before and after the recipe
08:40 Act Three — The decisions a person makes, in the recipe
08:55 Act Three — The decisions a person makes, in the recipe
09:07 Act Three — The decisions a person makes, in the recipe
09:22 Drift inside a running container
10:03 Act Four — Running it again
10:29 One kernel, shared
11:01 Act Five — The bill
11:29 What It Costs
12:02 Where You Have Seen It
12:32 How To Use It
13:00 The Java Classes
13:13 Docker — run a script as root inside a container
13:28 Scanner — the snapshot script
13:41 Scanner — a rule with its fix
13:54 Listeners — every interface, and the port in hex
14:04 Images — the decisions, in the recipe
14:18 Thanks for Watching
```

## Tags

linux hardening, docker, cis benchmarks, container security, file permissions, sshd_config, setuid, security misconfiguration, owasp top 10, java, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/linux-hardening-with-docker-explained.mp4` |
| Captions | `video/linux-hardening-with-docker-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Platform Hardening in Java - With Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (92)
- [ ] tags are at most 500 characters in total — ok (187)
- [ ] description is at most 5,000 characters — ok (569)
- [ ] chapters start at 00:00 and there are at least three
