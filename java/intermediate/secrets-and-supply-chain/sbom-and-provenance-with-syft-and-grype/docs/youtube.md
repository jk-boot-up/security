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
00:51 The Scenario
01:23 The Words
02:02 What the Letters Stand For
03:00 What the Letters Stand For (continued)
03:25 One package URL, and its fingerprint
04:11 An image goes in, syft writes the bill
05:05 Act One — What a bill of materials answers, and what it does not
05:22 Act One — What a bill of materials answers, and what it does not
05:30 Act One — What a bill of materials answers, and what it does not
05:47 The installed version, and where each advisory is fixed
06:46 Act Two — Scanning it against known advisories
07:07 Act Two — Scanning it against known advisories
07:23 Two bills compared, and the scans before and after
08:11 Act Three — Fixing it, and checking that it is fixed
08:31 Six requests to deploy, and the gate's answers
09:22 Act Four — A gate that reads the scan
09:30 Act Four — A gate that reads the scan
09:42 What a scan cannot see
10:30 Act Five — The bill
10:46 Act Five — The bill
10:56 What It Costs
11:30 Where You Have Seen It
12:04 How To Use It
12:35 The Java Classes
12:49 Lab — an image with one log4j version
13:04 Sbom — catalogue an image
13:16 Sbom — the libraries, by package URL
13:25 Scan — scan an image, or a bill alone
13:41 Gate — an exception covers a high advisory, until its day
13:51 Purl — type, namespace, name, version
14:01 Thanks for Watching
```

## Tags

syft, grype, sbom, cyclonedx, log4j, vulnerability scanning, supply chain, owasp top 10, docker, java, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/sbom-and-provenance-with-syft-and-grype-explained.mp4` |
| Captions | `video/sbom-and-provenance-with-syft-and-grype-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Secrets and Supply Chain Security in Java - With Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (98)
- [ ] tags are at most 500 characters in total — ok (143)
- [ ] description is at most 5,000 characters — ok (482)
- [ ] chapters start at 00:00 and there are at least three
