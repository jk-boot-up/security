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
02:03 What the Letters Stand For
03:19 What the Letters Stand For (continued)
03:39 One known problem, from advisory to action
04:44 What the store is built from
05:48 Act One — An inventory of what the store ships
06:11 Comparing versions, and reading a range
07:19 Act Two — Matching the inventory against known problems
07:32 Act Two — Matching the inventory against known problems
07:51 How 9.8 is made
09:08 Act Three — How bad, and what to fix first
09:21 Act Three — How bad, and what to fix first
09:44 Three findings, three deadlines, one breach
10:53 Act Four — Patch windows
11:13 One bit flipped, and the check that catches it
12:17 Act Five — Pinning, and checking what you download
12:24 Act Five — Pinning, and checking what you download
12:48 Five Questions, In Order
13:09 Where You Have Seen It
13:47 How To Use It
14:15 The Java Classes
14:27 SemVer — major, then minor, then patch
14:41 Audit — is the installed version in the range?
14:59 Audit — reachable first, then the highest score
15:13 Cvss31 — impact and exploitability
15:33 PatchWindow — days to patch
15:44 Pins — hash again, compare in constant time
16:02 Thanks for Watching
```

## Tags

patching, dependency management, sbom, cvss, supply chain security, checksums, vulnerability management, java, cybersecurity for beginners, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/patching-and-dependency-hygiene-explained.mp4` |
| Captions | `video/patching-and-dependency-hygiene-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Safe Habits in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (76)
- [ ] tags are at most 500 characters in total — ok (181)
- [ ] description is at most 5,000 characters — ok (718)
- [ ] chapters start at 00:00 and there are at least three
