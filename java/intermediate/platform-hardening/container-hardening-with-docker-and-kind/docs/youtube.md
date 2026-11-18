# YouTube sheet — Container Hardening with Docker and kind

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Container Hardening with Docker and kind: 13 Real Attacks, Pod Security Standards (CIS Benchmarks)

## Description

Container hardening measured with thirteen real attempts inside real Docker containers (write to the app, run a dropped file, become root, sniff the network, mount, read the Docker socket, the host's files, the host network and processes, memory and process limits, the secret in the environment) from the tutorial's docker run with 13 of 13 possible down to a hardened container with 1, then a real kind cluster with Pod Security Admission: a tutorial pod refused by baseline and restricted, a plain pod admitted by baseline and refused by restricted, a namespace that only warns, and the attacks that remain in the pods that are admitted.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/intermediate/platform-hardening/container-hardening-with-docker-and-kind

Chapters:

```
00:00 Introduction
00:51 The Scenario
01:21 The Words
02:16 What the Letters Stand For
03:09 What the Letters Stand For (continued)
04:04 What the Letters Stand For (continued)
04:27 How a probe reads the capability mask
05:27 No options: what the probes read, and what works
06:27 Act One — What docker run with no options gives an attacker
06:42 Act One — What docker run with no options gives an attacker
07:03 Act One — What docker run with no options gives an attacker
07:20 Act One — What docker run with no options gives an attacker
07:36 The tutorial's options, and thirteen attempts that work
08:26 Act Two — The configuration from the tutorial
08:39 Act Two — The configuration from the tutorial
08:50 Act Two — The configuration from the tutorial
09:03 Which attempts still work after each step
10:06 Act Three — One setting at a time
10:23 Act Three — One setting at a time
10:41 What the probes read, before and after
11:29 Act Four — The hardened container
11:43 Act Four — The hardened container
11:58 Act Four — The hardened container
12:07 Act Four — The hardened container
12:17 Small additions, and what each one opens
13:01 Act Five — Settings that look harmless
13:20 Act Five — Settings that look harmless
13:28 Which namespace admits which pod
14:29 Act Six — The same on Kubernetes, with Pod Security Standards on kind
14:42 Act Six — The same on Kubernetes, with Pod Security Standards on kind
15:01 Act Six — The same on Kubernetes, with Pod Security Standards on kind
15:20 What It Costs
15:54 Where You Have Seen It
16:26 How To Use It
17:02 The Java Classes
17:14 Probes — an attempt is a command that works or fails
17:31 Probes — what the probes read
17:42 Capabilities — one capability is one bit
17:57 Run — docker run, then docker exec
18:10 Cluster — a namespace that enforces a standard
18:21 Report — blocked and opened, from two lists
18:35 Thanks for Watching
```

## Tags

container hardening, docker, kind, kubernetes, pod security standards, seccomp, capabilities, read-only root, cis benchmarks, java, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/container-hardening-with-docker-and-kind-explained.mp4` |
| Captions | `video/container-hardening-with-docker-and-kind-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Platform Hardening in Java - With Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (98)
- [ ] tags are at most 500 characters in total — ok (172)
- [ ] description is at most 5,000 characters — ok (640)
- [ ] chapters start at 00:00 and there are at least three
