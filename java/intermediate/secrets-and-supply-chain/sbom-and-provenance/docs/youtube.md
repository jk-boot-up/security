# YouTube sheet — SBOMs and Build Provenance: What Is Inside, Who Built It, and From What

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

SBOMs and Build Provenance in Java (CycloneDX, SLSA, in-toto, DSSE)

## Description

SBOMs and build provenance in Java: an online store's release jar comes with a CycloneDX bill of materials that lists every library with its package URL and SHA-256, and finds a library changed after the build, but cannot say who built the jar or from which source. Signed provenance answers that: an in-toto statement with a SLSA Provenance predicate names the artifact by digest, the source repository, branch and commit, and the builder, signed with Ed25519 in a DSSE envelope. A deployment gate then refuses a build of a fork, an unreviewed branch, a laptop-signed build, a statement naming the wrong builder, an edited statement and a swapped jar. SLSA build levels are explained by what each adds, and reproducible builds let anyone rebuild the commit and check the digest. The bill: provenance helps only when checked, the builder's key must be protected, and an SBOM goes stale. Mapped to OWASP Top 10 2025 A03 and A08, SLSA, in-toto and NIST SSDF.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/intermediate/secrets-and-supply-chain/sbom-and-provenance

Chapters:

```
00:00 Introduction
00:59 The Scenario
01:22 The Words
01:54 Act One — What a bill of materials answers, and what it does not
02:34 Act Two — Provenance: who built it, from what
03:11 Act Three — Checking before deploying
03:55 Act Four — SLSA build levels, and builds that can be repeated
04:43 Act Five — The bill
05:16 Questions Before a Release Runs
05:44 Who Does What
06:13 Where You Have Seen It
06:40 How To Use It
07:08 Thanks for Watching
```

## Tags

sbom, cyclonedx, slsa, provenance, in-toto, dsse, software supply chain, reproducible builds, java, application security

## Files

| What | File |
| --- | --- |
| Video | `video/sbom-and-provenance-explained.mp4` |
| Captions | `video/sbom-and-provenance-explained.srt` |
| Thumbnail | `docs/thumbnail.png` (1280×720) |

## Settings

Public with instant Premiere; not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Security Foundations in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (67)
- [ ] tags are at most 500 characters in total — ok (111)
- [ ] description is at most 5,000 characters — ok (956)
- [ ] chapters start at 00:00 and there are at least three
