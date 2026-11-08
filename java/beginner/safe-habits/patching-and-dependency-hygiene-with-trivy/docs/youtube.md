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
02:00 What the Letters Stand For
03:11 From the build to a decision
04:04 The lock file, line by line
05:02 Act One — Pinning exactly what the build uses
05:19 From a lock file line to a package URL
06:19 Act Two — A bill of materials a scanner can read
06:31 What Trivy found
07:20 Act Three — What the scanner reported
07:44 Which version to move to
08:44 Act Four — What to fix first, and to which version
09:02 Checksums that stop a changed download
09:53 Act Five — Checking what is downloaded, and automating the updates
10:02 Act Five — Checking what is downloaded, and automating the updates
10:19 What Each Tool Answers
10:46 Where You Have Seen It
11:13 How To Use It
11:36 The Java Classes
11:49 Dependency — the package URL
12:02 GradleLockfile — one line, three parts
12:14 Vulnerability — the nearest fix
12:26 Triage — keep the highest nearest fix
12:42 VerificationMetadata — which files differ
12:56 Thanks for Watching
```

## Tags

trivy, sbom, cyclonedx, gradle dependency locking, gradle dependency verification, dependabot, renovate, log4shell, java, cybersecurity for beginners, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/patching-and-dependency-hygiene-with-trivy-explained.mp4` |
| Captions | `video/patching-and-dependency-hygiene-with-trivy-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Safe Habits in Java - With Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (86)
- [ ] tags are at most 500 characters in total — ok (191)
- [ ] description is at most 5,000 characters — ok (637)
- [ ] chapters start at 00:00 and there are at least three
