# YouTube sheet — Dependency Confusion with Gradle

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Dependency Confusion with Gradle: Content Filters and Dependency Verification (OWASP A03)

## Description

Dependency confusion with real Gradle 9 builds against a private and a public Maven repository on loopback ports: a version range that takes the attacker's 99.0.0 from the public repository, an exact version taken from the first repository that has it, content filters that keep an internal group out of the public repository and show zero requests for it, dependency verification metadata that refuses an unlisted version and the same version with other bytes, a filter for com.shop that does not cover com.shop.tools, and checksums written from a compromised build.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/intermediate/secrets-and-supply-chain/dependency-confusion-with-gradle

Chapters:

```
00:00 Introduction
01:36 The Scenario
01:57 Act One — A build that works
02:14 Act Two — Somebody registers the company's name in the public repository
02:30 Act Three — The repository says which names it may answer for
02:49 Act Four — Checksums that the build checks
03:06 Act Five — The bill
03:24 What It Costs
03:45 Who Does What
04:03 How To Use It
04:25 Thanks for Watching
```

## Tags

dependency confusion, gradle, repository content filtering, dependency verification, supply chain, maven, owasp top 10, a03, java

## Files

| What | File |
| --- | --- |
| Video | `video/dependency-confusion-with-gradle-explained.mp4` |
| Captions | `video/dependency-confusion-with-gradle-explained.srt` |
| Thumbnail | `docs/thumbnail.png` (1280×720) |

## Settings

Public with instant Premiere; not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Security Foundations in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (89)
- [ ] tags are at most 500 characters in total — ok (121)
- [ ] description is at most 5,000 characters — ok (567)
- [ ] chapters start at 00:00 and there are at least three
