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
01:31 The Scenario
01:47 Act One — What the scan finds in a typical image
02:10 Act Two — The fixes a machine can make, and where they live
02:31 Act Three — The decisions a person makes, in the recipe
02:49 Act Four — Running it again
03:04 Act Five — The bill
03:21 What It Costs
03:42 Who Does What
04:00 Where You Have Seen It
04:17 How To Use It
04:36 Thanks for Watching
```

## Tags

linux hardening, docker, cis benchmarks, container security, file permissions, sshd_config, setuid, security misconfiguration, owasp top 10, java

## Files

| What | File |
| --- | --- |
| Video | `video/linux-hardening-with-docker-explained.mp4` |
| Captions | `video/linux-hardening-with-docker-explained.srt` |
| Thumbnail | `docs/thumbnail.png` (1280×720) |

## Settings

Public with instant Premiere; not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Security Foundations in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (92)
- [ ] tags are at most 500 characters in total — ok (136)
- [ ] description is at most 5,000 characters — ok (569)
- [ ] chapters start at 00:00 and there are at least three
