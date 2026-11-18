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
00:55 The Scenario
01:26 The Words
02:07 What the Letters Stand For
03:08 Gradle asks every repository
04:00 Two repositories, three dependencies, and who published each jar
04:47 Act One — A build that works
05:09 A range takes the highest; a pin takes the first copy
06:00 Act Two — Somebody registers the store's name in the public repository
06:20 Act Two — Somebody registers the store's name in the public repository
06:36 The filters, and the public repository's log
07:31 Act Three — The repository says which names it may answer for
07:48 Act Three — The repository says which names it may answer for
08:05 The metadata's checksum against what a repository serves
08:56 Act Four — Checksums that the build checks
09:13 Act Four — Checksums that the build checks
09:31 What a filter covers, and checksums written too late
10:20 Act Five — The bill
10:36 Act Five — The bill
10:53 What It Costs
11:27 Where You Have Seen It
11:58 How To Use It
12:29 The Java Classes
12:43 Repo — remember the path, then answer
12:56 Build — who published each resolved jar
13:09 DependencyConfusionDemo — the two filters
13:24 DependencyConfusionDemo — write the checksums once
13:32 VersionSteps — in range, then the highest
13:44 GroupFilter — exact group, or group and subgroups
13:59 Checksums — the SHA-256 of the bytes
14:10 Thanks for Watching
```

## Tags

dependency confusion, gradle, repository content filtering, dependency verification, supply chain, maven, owasp top 10, a03, java, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/dependency-confusion-with-gradle-explained.mp4` |
| Captions | `video/dependency-confusion-with-gradle-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Secrets and Supply Chain Security in Java - With Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (89)
- [ ] tags are at most 500 characters in total — ok (172)
- [ ] description is at most 5,000 characters — ok (567)
- [ ] chapters start at 00:00 and there are at least three
