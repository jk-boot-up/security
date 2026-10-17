# YouTube sheet — Backups and Recovery

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Backups and Recovery in Java (3-2-1, Ransomware, RPO and RTO)

## Description

Backups and recovery in Java: the last line of defence when everything else fails. Explained with an online store, like a fire drill, because a plan nobody has practised is only a hope. We make full and incremental backups with a manifest of SHA-256 fingerprints, catch a changed and a deleted file, show a backup that passes its checks and still cannot be restored, grade a setup against the 3-2-1 rule, prune snapshots by a retention policy, watch ransomware encrypt the shop and its mounted backup folder, restore from a store that only adds versions, work out recovery point and recovery time from a timeline, and seal the archive with AES-GCM, because a backup is a copy of every secret.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/beginner/safe-habits/backups-and-recovery

Chapters:

```
00:00 Introduction
00:55 The Scenario
01:17 The Words
01:51 Act One — Full and incremental backups, with a manifest
02:33 Act Two — Can the backup be trusted? Only a restore test says
03:12 Act Three — The 3-2-1 rule, and how long to keep backups
03:57 Act Four — Ransomware, and the copy it cannot change
04:45 Act Five — How much can be lost, how long it takes, and backups as secrets
05:39 What Makes a Backup Good
06:05 Who Does What
06:37 Where You Have Seen It
07:01 How To Use It
07:27 Thanks for Watching
```

## Tags

backups, disaster recovery, ransomware, 3-2-1 rule, rpo rto, restore test, immutable backups, java, cybersecurity for beginners

## Files

| What | File |
| --- | --- |
| Video | `video/backups-and-recovery-explained.mp4` |
| Captions | `video/backups-and-recovery-explained.srt` |
| Thumbnail | `docs/thumbnail.png` (1280×720) |

## Settings

Public with instant Premiere; not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Security Foundations in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (61)
- [ ] tags are at most 500 characters in total — ok (119)
- [ ] description is at most 5,000 characters — ok (692)
- [ ] chapters start at 00:00 and there are at least three
