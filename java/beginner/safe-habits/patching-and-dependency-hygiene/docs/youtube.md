# YouTube sheet — Patching and Dependency Hygiene

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Patching and Dependency Hygiene in Java (SBOM, CVSS, Patch Windows, Pinning)

## Description

Patching and dependency hygiene in Java: most of a program is libraries somebody else wrote, and their bugs become yours. Explained with an online store, like a restaurant that must know every ingredient in every dish before a supplier announces a recall. We list what the store ships in a bill of materials, follow dependencies of dependencies, compare versions as numbers and match them against known problems, score each problem with the real CVSS equation, decide what to fix first by asking whether the store even uses the flawed feature, measure fix times against deadlines on a simulated clock, and pin the checksum of every artifact so a changed download is refused. All the problems in the demo are fictional.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/beginner/safe-habits/patching-and-dependency-hygiene

Chapters:

```
00:00 Introduction
01:11 The Scenario
01:33 The Words
02:03 Act One — An inventory of what the store ships
02:36 Act Two — Matching the inventory against known problems
03:22 Act Three — How bad, and what to fix first
04:15 Act Four — Patch windows
05:04 Act Five — Pinning, and checking what you download
05:49 Five Questions, In Order
06:09 Who Does What
06:40 Where You Have Seen It
07:18 How To Use It
07:47 Thanks for Watching
```

## Tags

patching, dependency management, sbom, cvss, supply chain security, checksums, vulnerability management, java, cybersecurity for beginners

## Files

| What | File |
| --- | --- |
| Video | `video/patching-and-dependency-hygiene-explained.mp4` |
| Captions | `video/patching-and-dependency-hygiene-explained.srt` |
| Thumbnail | `docs/thumbnail.png` (1280×720) |

## Settings

Public with instant Premiere; not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Security Foundations in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (76)
- [ ] tags are at most 500 characters in total — ok (130)
- [ ] description is at most 5,000 characters — ok (718)
- [ ] chapters start at 00:00 and there are at least three
