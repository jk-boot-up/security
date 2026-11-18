# YouTube sheet — Secure Pipeline

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Secure Pipeline: Reproducible Builds, Signing, Provenance and Gates That Fail Closed

## Description

A local release pipeline in Java for a bank's Gradle library: reproducible builds, cosign signatures checked again with the JDK, SLSA-style provenance, gates for secrets, dependencies, tests and signatures that fail closed when a tool is missing or crashes, and a deploy step that refuses forged releases.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/advanced/devsecops-pipelines/secure-pipeline

Chapters:

```
00:00 Introduction
00:45 The Scenario
01:15 The Words
01:54 What the Letters Stand For
02:54 What the Letters Stand For (continued)
03:23 One release, four files
04:26 One commit, two checkouts, three settings
05:41 Act One — Build it twice
06:03 Act One — Build it twice
06:33 A signature over one digest
07:43 Act Two — Sign it
08:09 Act Two — Sign it
08:29 What, from what, by whom
09:33 Act Three — Say where it came from
09:52 Act Three — Say where it came from
10:08 Each change against the four gates
11:11 Act Four — Four gates, six planted flaws
11:30 Act Four — Four gates, six planted flaws
11:43 Act Four — Four gates, six planted flaws
11:53 Act Four — Four gates, six planted flaws
12:23 No answer must mean no
13:28 Act Five — A gate whose tool cannot answer
13:46 Act Five — A gate whose tool cannot answer
14:04 Four releases against seven checks
15:13 Act Six — Deploy only what verifies
15:29 Act Six — Deploy only what verifies
15:52 Act Six — Deploy only what verifies
16:00 What It Costs
16:31 Who Does What
17:12 Where You Have Seen It
17:42 How To Use It
18:15 The Java Classes
18:27 Pipeline — run a gate, stop or go on
18:44 Gate — an error is not a pass
19:00 Reproducibility — other bytes, other times
19:14 SignatureSteps — SHA-256, then ECDSA
19:33 Provenance — the commit, the tree and the subject
19:45 Verifier — signature, digest, commit
19:59 Tools — refuse a download that does not match
20:13 Thanks for Watching
```

## Tags

devsecops, secure pipeline, reproducible builds, cosign, slsa, provenance, supply chain, gitleaks, trivy, fail closed, gradle, java, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/secure-pipeline-explained.mp4` |
| Captions | `video/secure-pipeline-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: DevSecOps Pipelines in Java - With Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (84)
- [ ] tags are at most 500 characters in total — ok (171)
- [ ] description is at most 5,000 characters — ok (305)
- [ ] chapters start at 00:00 and there are at least three
