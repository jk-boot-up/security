# YouTube sheet — SBOMs and Scanning with syft and grype

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

SBOMs and Scanning with syft and grype: log4j, a Gate, Exceptions and Blind Spots (OWASP A03, A06)

## Description

SBOMs and vulnerability scanning with the real syft and grype commands: images built here with log4j 2.14.1, 2.16.0 and 2.25.5, a CycloneDX bill of materials with package URLs, grype matching log4j-shell and its follow-ups with the versions that fix them, a scan of the bill alone, a diff of two bills, a deployment gate with time-limited exceptions that cannot cover a critical advisory, a renamed jar that no scanner finds, a stale bill, and the age of the vulnerability database.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/intermediate/secrets-and-supply-chain/sbom-and-provenance-with-syft-and-grype

Chapters:

```
00:00 Introduction
01:30 The Scenario
01:50 Act One — What a bill of materials answers
02:10 Act Two — Scanning it against known advisories
02:28 Act Three — Fixing it, and checking that it is fixed
02:49 Act Four — A gate that reads the scan
03:11 Act Five — The bill
03:29 What It Costs
03:56 Who Does What
04:12 Where You Have Seen It
04:29 How To Use It
04:50 Thanks for Watching
```

## Tags

syft, grype, sbom, cyclonedx, log4j, vulnerability scanning, supply chain, owasp top 10, docker, java

## Files

| What | File |
| --- | --- |
| Video | `video/sbom-and-provenance-with-syft-and-grype-explained.mp4` |
| Captions | `video/sbom-and-provenance-with-syft-and-grype-explained.srt` |
| Thumbnail | `docs/thumbnail.png` (1280×720) |

## Settings

Public with instant Premiere; not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Security Foundations in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (98)
- [ ] tags are at most 500 characters in total — ok (92)
- [ ] description is at most 5,000 characters — ok (482)
- [ ] chapters start at 00:00 and there are at least three
