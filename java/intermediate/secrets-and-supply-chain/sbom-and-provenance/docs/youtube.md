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
01:28 The Words
02:09 What the Letters Stand For
03:14 What the Letters Stand For (continued)
03:37 One library in the SBOM
04:30 What the SBOM records, and what it cannot say
05:27 Act One — What a bill of materials answers, and what it does not
05:38 Act One — What a bill of materials answers, and what it does not
06:07 Act One — What a bill of materials answers, and what it does not
06:29 The statement, the bytes that are signed, and the envelope
07:35 Act Two — Provenance: who built it, from what
07:49 Act Two — Provenance: who built it, from what
08:17 Act Two — Provenance: who built it, from what
08:31 Eight releases against the gate's seven checks
09:33 Act Three — Checking before deploying
09:43 Act Three — Checking before deploying
09:56 Act Three — Checking before deploying
10:07 Act Three — Checking before deploying
10:28 Build levels, and builds that can be repeated
11:33 Act Four — SLSA build levels, and builds that can be repeated
11:49 Act Four — SLSA build levels, and builds that can be repeated
11:59 Act Four — SLSA build levels, and builds that can be repeated
12:12 What still goes wrong
13:04 Act Five — The bill
13:30 Questions Before a Release Runs
13:58 What It Costs
14:34 Where You Have Seen It
15:00 How To Use It
15:29 The Java Classes
15:40 Sbom — one component: package URL and SHA-256
15:52 Sbom — is this the SBOM for this jar?
16:05 Provenance — the subject, the source and the builder
16:19 Dsse — the pre-authentication encoding
16:29 Dsse — open: the type, then the signature
16:39 Policy — a refusal names its check
16:53 Pipeline — the level from three facts
17:06 Archive — sorted entries, the commit's time
17:18 Thanks for Watching
```

## Tags

sbom, cyclonedx, slsa, provenance, in-toto, dsse, software supply chain, reproducible builds, java, application security, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/sbom-and-provenance-explained.mp4` |
| Captions | `video/sbom-and-provenance-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Secrets and Supply Chain Security in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (67)
- [ ] tags are at most 500 characters in total — ok (162)
- [ ] description is at most 5,000 characters — ok (956)
- [ ] chapters start at 00:00 and there are at least three
