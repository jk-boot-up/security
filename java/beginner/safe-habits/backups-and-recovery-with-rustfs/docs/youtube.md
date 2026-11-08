# YouTube sheet — Backups and Recovery with RustFS, an S3-Compatible Object Store

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Backups and Recovery with RustFS, an S3-Compatible Store (Versioning, Object Lock, Write-Only Keys)

## Description

Backups and recovery with RustFS, an S3-compatible object store, in Java: the same S3 API as Amazon S3, run locally in Docker. Explained with an online store, like a safe deposit box where the courier can post envelopes through a slot but cannot open the box. We watch ransomware overwrite a file and restore the older version, lock a backup with Object Lock so that not even the root account can delete it, give the backup job an identity that can only add objects, catch a damaged upload with a SHA-256 checksum, and finish with the bill: storage grows with every version, a compliance lock cannot be undone, and the root account is the real key.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/beginner/safe-habits/backups-and-recovery-with-rustfs

Chapters:

```
00:00 Introduction
01:02 The Scenario
01:18 The Words
01:43 What the Letters Stand For
03:14 What the Letters Stand For (continued)
03:25 One nightly backup, and the guards around it
04:22 The versions of orders.csv, step by step
05:18 Act One — Versioning: an overwrite does not destroy the old data
05:34 Act One — Versioning: an overwrite does not destroy the old data
05:51 What root may do to a locked version
06:53 Act Two — Object Lock: nobody can delete a locked version
07:00 Act Two — Object Lock: nobody can delete a locked version
07:22 One action, on one bucket
08:14 Act Three — The backup job gets an identity that can only add objects
08:30 The checksum travels with the bytes
09:19 Act Four — A checksum catches a damaged backup
09:26 Act Four — A checksum catches a damaged backup
09:42 History is kept in full, for thirty days
10:38 Act Five — Keeping history without keeping it for ever, and the cost
10:52 Act Five — Keeping history without keeping it for ever, and the cost
11:08 Three Protections
11:34 Where You Have Seen It
11:58 How To Use It
12:22 The Java Classes
12:39 ObjectStoreContainer — both images pinned to exact versions
12:50 ObjectStoreContainer — the backup job's policy: one action
13:04 Store — every upload carries its SHA-256
13:19 Store — read one version by its id
13:27 Store — lock a version until a date
13:40 Store — old versions expire after some days
13:51 Thanks for Watching
```

## Tags

backups, ransomware, object lock, s3, rustfs, versioning, least privilege, docker, java, cybersecurity for beginners, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/backups-and-recovery-with-rustfs-explained.mp4` |
| Captions | `video/backups-and-recovery-with-rustfs-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Safe Habits in Java - With Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (99)
- [ ] tags are at most 500 characters in total — ok (158)
- [ ] description is at most 5,000 characters — ok (648)
- [ ] chapters start at 00:00 and there are at least three
