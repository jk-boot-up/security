# YouTube sheet — Container Hardening

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Container Hardening: What a Compromised Process Can Reach (Docker and Kubernetes)

## Description

Container hardening in Java for a containerised web shop: an attacker model asks thirteen questions of a container's settings (read secrets, tamper with the app, become root, use the Docker socket, reach the host, exhaust memory...); docker run defaults allow 7, a tutorial configuration all 13, and one setting at a time (no host access, seccomp, no capabilities, non-root, no new privileges, read-only root, limits) brings it to 1, at the Restricted Pod Security level, printed as Docker flags and a Kubernetes securityContext.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/intermediate/platform-hardening/container-hardening

Chapters:

```
00:00 Introduction
00:54 The Scenario
01:29 The Words
02:19 What the Letters Stand For
03:13 What the Letters Stand For (continued)
04:02 One hijacked process, thirteen questions
05:04 Docker's defaults, and what they give an attacker
06:10 Act One — What 'docker run' with no options gives an attacker
06:28 Act One — What 'docker run' with no options gives an attacker
06:48 Act One — What 'docker run' with no options gives an attacker
07:05 Act One — What 'docker run' with no options gives an attacker
07:23 Six flags, six reasons it is not even Baseline
08:24 Act Two — The configuration from the tutorial
08:37 Act Two — The configuration from the tutorial
08:49 Act Two — The configuration from the tutorial
09:04 Which attacks are left after each step
10:09 Act Three — One setting at a time
10:27 Act Three — One setting at a time
10:44 Every Pod Security check, before and after
11:38 Act Four — The hardened container
11:56 Act Four — The hardened container
12:07 Act Four — The hardened container
12:28 Two small additions, and what each one undoes
13:18 Act Five — Settings that look harmless
13:37 Act Five — Settings that look harmless
13:50 What It Costs
14:28 Where You Have Seen It
15:01 How To Use It
15:36 The Java Classes
15:49 ContainerSpec — root, and a capability
16:10 Attacker — one rule per attack
16:27 PodSecurityLevel — a check per rule
16:35 PodSecurityLevel — the highest level that passes
16:46 Renderer — flags from the fields
16:56 ContainerDemo — what a step blocked
17:05 Thanks for Watching
```

## Tags

container security, docker, kubernetes, pod security standards, capabilities, seccomp, java, application security, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/container-hardening-explained.mp4` |
| Captions | `video/container-hardening-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Platform Hardening in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (81)
- [ ] tags are at most 500 characters in total — ok (157)
- [ ] description is at most 5,000 characters — ok (529)
- [ ] chapters start at 00:00 and there are at least three
