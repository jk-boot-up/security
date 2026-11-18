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
00:50 The Scenario
01:22 The Words
02:12 What the Letters Stand For
03:01 What the Letters Stand For (continued)
03:32 One backup, three kinds of bucket
04:30 A plain bucket: 24 backups, overwritten, then deleted
05:30 Act One — A plain bucket
05:50 Act One — A plain bucket
06:06 Versioning: one key, step by step
07:03 Act Two — Versioning only
07:24 Act Two — Versioning only
07:41 Object lock: who is refused, and the one gap
08:49 Act Three — Object lock in COMPLIANCE mode
09:11 Act Three — Object lock in COMPLIANCE mode
09:34 One job per account, and a store the attacker's keys do not open
10:38 Act Four — Isolation and an air-gapped copy
10:57 Act Four — Isolation and an air-gapped copy
11:15 Act Four — Isolation and an air-gapped copy
11:33 Entropy, worked out on one 4 KiB block
12:52 Act Five — The guard: a canary file and entropy
13:11 Act Five — The guard: a canary file and entropy
13:37 The poisoned 10:00 backup, and the run that is restored
14:48 Act Six — Ransomware and the poisoned backup
15:06 Act Six — Ransomware and the poisoned backup
15:27 Act Six — Ransomware and the poisoned backup
15:45 How long the way back takes, worked out
16:54 Act Seven — The restore exercise
17:12 Act Seven — The restore exercise
17:30 Act Seven — The restore exercise
17:44 How much data a backup schedule loses
18:35 What It Costs
19:14 Where You Have Seen It
19:40 How To Use It
20:13 The Java Classes
20:26 Bucket — object lock with a default retention in COMPLIANCE mode
20:41 Bucket — every attempt becomes ok, an error code, or unreachable
20:52 Policies — the writer may add and list; the recovery account may read
21:03 Guard — three detectors, any one stops the backup
21:17 Entropy — the sum of -p x log2(p) over the byte values
21:30 BackupJob — a guarded job looks first and writes nothing
21:39 Restore — a run must match its manifest and still look like a ledger
21:48 Recovery — the time from the steps and backups the restore really took
22:02 Thanks for Watching
```

## Tags

ransomware, immutable backups, object lock, worm, s3, rustfs, air gap, rto, rpo, backup security, java, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/ransomware-resilience-explained.mp4` |
| Captions | `video/ransomware-resilience-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Resilience and Recovery in Java - With Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (79)
- [ ] tags are at most 500 characters in total — ok (143)
- [ ] description is at most 5,000 characters — ok (314)
- [ ] chapters start at 00:00 and there are at least three
