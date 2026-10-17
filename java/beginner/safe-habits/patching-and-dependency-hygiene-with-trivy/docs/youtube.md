# YouTube sheet — Patching and Dependency Hygiene with Trivy and Gradle

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Patching and Dependency Hygiene with Trivy and Gradle (SBOM, Lock Files, Verification)

## Description

Patching and dependency hygiene with Trivy and Gradle, in Java: the tools that do the checking for you. Explained with an online store whose libraries include the famous log4j version. We pin every library with a Gradle lock file, write a CycloneDX bill of materials, let the real Trivy scanner read it in Docker, compare its report with our own checks, decide what to fix first and to which version, upgrade and see the critical problems disappear, and let Gradle's dependency verification refuse a download whose checksum was changed. We finish with the bill: a scanner is only as good as its database, and every update needs a review.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/beginner/safe-habits/patching-and-dependency-hygiene-with-trivy

Chapters:

```
00:00 Introduction
01:11 The Scenario
01:32 The Words
02:00 Act One — Pinning exactly what the build uses
02:26 Act Two — A bill of materials a scanner can read
02:51 Act Three — What the scanner reported
03:18 Act Four — What to fix first, and to which version
03:48 Act Five — Checking what is downloaded, and automating the updates
04:22 What Each Tool Answers
04:48 Who Does What
05:18 Where You Have Seen It
05:45 How To Use It
06:08 Thanks for Watching
```

## Tags

trivy, sbom, cyclonedx, gradle dependency locking, gradle dependency verification, dependabot, renovate, log4shell, java, cybersecurity for beginners

## Files

| What | File |
| --- | --- |
| Video | `video/patching-and-dependency-hygiene-with-trivy-explained.mp4` |
| Captions | `video/patching-and-dependency-hygiene-with-trivy-explained.srt` |
| Thumbnail | `docs/thumbnail.png` (1280×720) |

## Settings

Public with instant Premiere; not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Security Foundations in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (86)
- [ ] tags are at most 500 characters in total — ok (140)
- [ ] description is at most 5,000 characters — ok (637)
- [ ] chapters start at 00:00 and there are at least three
