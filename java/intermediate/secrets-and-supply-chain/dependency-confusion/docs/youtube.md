# YouTube sheet — Dependency Confusion

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Dependency Confusion: How a Public Package Hijacks a Private Name (and How to Stop It)

## Description

Dependency confusion in Java, for an online store's build: an attacker registers the store's internal package name in a public registry, a resolver that takes the highest version installs it and its install script runs on the build machine, repository order fails for a new or copied name, and the fixes: names decide which repository may supply them and fail closed, a lockfile with digests, and a watch for internal names in public.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/intermediate/secrets-and-supply-chain/dependency-confusion

Chapters:

```
00:00 Introduction
00:54 The Scenario
01:31 The Words
02:19 What the Letters Stand For
03:27 One name, two places
04:18 The build that works, and how its lockfile pins the bytes
05:13 Act One — A build that works
05:40 Act One — A build that works
05:59 Two versions, compared number by number
06:53 Act Two — Somebody registers the store's name in the public registry
07:11 Act Two — Somebody registers the store's name in the public registry
07:24 The first repository that has the name answers
08:23 Act Three — Repository order helps only until a name is missing
08:42 Act Three — Repository order helps only until a name is missing
08:53 Act Three — Repository order helps only until a name is missing
09:12 Each name goes to one repository only
09:57 Act Four — Names decide where a package may come from
10:12 Act Four — Names decide where a package may come from
10:24 The lockfile against what the build found
11:18 Act Five — Locked digests, and watching the names
11:35 Act Five — Locked digests, and watching the names
11:48 What It Costs
12:24 Where You Have Seen It
12:59 How To Use It
13:32 The Java Classes
13:45 HighestVersionResolver — one stream over all repositories
13:59 Version — the first number that differs decides
14:14 FirstRepositoryResolver — the first match wins
14:27 ContentFilteringResolver — internal names, private repositories only
14:43 Artifact — three lines, then SHA-256
14:55 VerifyingResolver — compare with the lockfile
15:07 Thanks for Watching
```

## Tags

dependency confusion, supply chain, gradle, repository, lockfile, dependency verification, java, application security, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/dependency-confusion-explained.mp4` |
| Captions | `video/dependency-confusion-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Secrets and Supply Chain Security in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (86)
- [ ] tags are at most 500 characters in total — ok (161)
- [ ] description is at most 5,000 characters — ok (434)
- [ ] chapters start at 00:00 and there are at least three
