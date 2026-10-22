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
01:50 The Scenario
02:12 Act One — What docker run with no options gives an attacker
02:32 Act Two — The configuration from the tutorial
02:50 Act Three — One setting at a time
03:08 Act Four — The hardened container
03:28 Act Five — The same on Kubernetes, with Pod Security Standards on kind
03:51 What It Costs
04:17 Who Does What
04:36 Where You Have Seen It
04:52 How To Use It
05:19 Thanks for Watching
```

## Tags

container hardening, docker, kind, kubernetes, pod security standards, seccomp, capabilities, read-only root, cis benchmarks, java

## Files

| What | File |
| --- | --- |
| Video | `video/container-hardening-with-docker-and-kind-explained.mp4` |
| Captions | `video/container-hardening-with-docker-and-kind-explained.srt` |
| Thumbnail | `docs/thumbnail.png` (1280×720) |

## Settings

Public with instant Premiere; not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Security Foundations in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (98)
- [ ] tags are at most 500 characters in total — ok (121)
- [ ] description is at most 5,000 characters — ok (640)
- [ ] chapters start at 00:00 and there are at least three
