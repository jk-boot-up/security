# YouTube sheet — IaC Scanning

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

IaC Scanning: Writing Rules, Comparing Checkov, Trivy and KICS, Exceptions and Baselines

## Description

Infrastructure-as-code scanning for an online store's Kubernetes manifests: eight rules written in Java and tested with their own samples, Checkov, Trivy and KICS compared on the same files, the password all three miss, exceptions with an owner and an end date, and a baseline that fails the build only on new findings.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/advanced/infrastructure-as-code-security/iac-scanning

Chapters:

```
00:00 Introduction
00:57 The Scenario
01:29 The Words
02:11 What the Letters Stand For
03:12 What the Letters Stand For (continued)
03:26 One container, eight questions
04:27 Thirteen findings before the fixes, one after
05:26 Act One — Eight rules in Java, on the store's manifests
05:46 Act One — Eight rules in Java, on the store's manifests
06:00 Act One — Eight rules in Java, on the store's manifests
06:15 A rule's own sample catches its mistake
07:17 Act Two — Testing the rules with their own samples
07:36 Act Two — Testing the rules with their own samples
07:54 Act Two — Testing the rules with their own samples
08:03 Our eight rules against Checkov, Trivy and KICS
08:58 Act Three — Three scanners on the same files
09:11 Act Three — Three scanners on the same files
09:38 A waiver that ends
10:35 Act Four — Exceptions: a waiver with an owner and an end
10:56 Act Four — Exceptions: a waiver with an owner and an end
11:11 Only the new findings fail the build
11:55 Act Five — A baseline in the build
12:11 Act Five — A baseline in the build
12:29 What It Costs
13:01 Where You Have Seen It
13:30 How To Use It
14:07 The Java Classes
14:20 Rules — a rule and its two samples
14:33 Rules — reading the tag
14:47 Engine — one finding per broken rule
15:01 RuleTester — must fire, must stay quiet
15:16 Scanners — Trivy from a pinned image
15:33 Waiver — valid, the same finding, and not ended
15:44 Baseline — only new findings count
15:56 Thanks for Watching
```

## Tags

iac scanning, kubernetes security, checkov, trivy, kics, policy as code, false positives, devsecops, shift left, java, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/iac-scanning-explained.mp4` |
| Captions | `video/iac-scanning-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Infrastructure as Code Security in Java - With Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (88)
- [ ] tags are at most 500 characters in total — ok (159)
- [ ] description is at most 5,000 characters — ok (319)
- [ ] chapters start at 00:00 and there are at least three
