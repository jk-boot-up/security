# YouTube sheet — Ransomware Resilience

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Ransomware Resilience: Immutable Backups, Isolated Recovery and a Timed Restore

## Description

Ransomware resilience for a bank's ledger backups in Java with the AWS SDK against RustFS: object lock in COMPLIANCE mode, versioning, separate writer and recovery credentials, an air-gapped copy, a guard with a canary file and entropy that stops poisoned backups, and a restore exercise with computed RTO and RPO.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/advanced/resilience-and-recovery/ransomware-resilience

Chapters:

```
00:00 Introduction
02:38 The Scenario
03:05 Act One — A plain bucket
03:17 Act Two — Versioning only
03:27 Act Three — Object lock in COMPLIANCE mode
03:37 Act Four — Isolation and an air-gapped copy
03:47 Act Five — Ransomware and the poisoned backup
04:00 Act Six — The restore exercise
04:14 What It Costs
05:13 Who Does What
05:38 Where You Have Seen It
05:56 How To Use It
06:23 Thanks for Watching
```

## Tags

ransomware, immutable backups, object lock, worm, s3, rustfs, air gap, rto, rpo, backup security, java

## Files

| What | File |
| --- | --- |
| Video | `video/ransomware-resilience-explained.mp4` |
| Captions | `video/ransomware-resilience-explained.srt` |
| Thumbnail | `docs/thumbnail.png` (1280×720) |

## Settings

Public with instant Premiere; not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Security Foundations in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (79)
- [ ] tags are at most 500 characters in total — ok (92)
- [ ] description is at most 5,000 characters — ok (314)
- [ ] chapters start at 00:00 and there are at least three
