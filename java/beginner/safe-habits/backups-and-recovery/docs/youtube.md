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
01:51 What the Letters Stand For
03:05 One file, from the shop to a tested restore
04:06 Which backup stored each file
05:02 Act One — Full and incremental backups, with a manifest
05:17 Act One — Full and incremental backups, with a manifest
05:33 The manifest check, then the restore test
06:38 Act Two — Can the backup be trusted? Only a restore test says
06:51 Act Two — Can the backup be trusted? Only a restore test says
07:03 Where the copies live, and which ones are kept
08:18 Act Three — The 3-2-1 rule, and how long to keep backups
08:30 Act Three — The 3-2-1 rule, and how long to keep backups
08:53 Four copies, one attack
10:02 Act Four — Ransomware, and the copy it cannot change
10:12 Act Four — Ransomware, and the copy it cannot change
10:35 Recovery point and recovery time, on one day
11:44 Act Five — Recovery point, recovery time, and a sealed backup
12:05 Act Five — Recovery point, recovery time, and a sealed backup
12:21 A backup is a copy of every secret
13:17 What Makes a Backup Good
13:43 Where You Have Seen It
14:07 How To Use It
14:33 The Java Classes
14:46 BackupStore — incremental: only files whose hash changed
15:01 BackupStore — restore: walk back along the chain
15:16 Manifest — missing, or changed
15:35 RestoreTest — is there a usable orders file?
15:53 ThreeTwoOne — copies, media, off site, locked
16:11 Retention — the newest Sundays, and the 1st of each month
16:30 Ransomware — append, never overwrite
16:44 Objectives — what is lost, and how long
17:01 BackupVault — the archive's name is sealed in
17:22 Thanks for Watching
```

## Tags

backups, disaster recovery, ransomware, 3-2-1 rule, rpo rto, restore test, immutable backups, java, cybersecurity for beginners, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/backups-and-recovery-explained.mp4` |
| Captions | `video/backups-and-recovery-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Safe Habits in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (61)
- [ ] tags are at most 500 characters in total — ok (170)
- [ ] description is at most 5,000 characters — ok (692)
- [ ] chapters start at 00:00 and there are at least three
