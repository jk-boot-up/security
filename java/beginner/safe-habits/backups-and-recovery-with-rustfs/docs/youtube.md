# YouTube sheet — Backups and Recovery with RustFS, an S3-Compatible Object Store

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Backups and Recovery with RustFS, an S3-Compatible Object Store (Versioning, Object Lock, Write-Only Keys)

## Description

Backups and recovery with RustFS, an S3-compatible object store, in Java: the same S3 API as Amazon S3, run locally in Docker. Explained with an online store, like a safe deposit box where the courier can post envelopes through a slot but cannot open the box. We watch ransomware overwrite a file and restore the older version, lock a backup with Object Lock so that not even the root account can delete it, give the backup job an identity that can only add objects, catch a damaged upload with a SHA-256 checksum, and finish with the bill: storage grows with every version, a compliance lock cannot be undone, and the root account is the real key.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/beginner/safe-habits/backups-and-recovery-with-rustfs

Chapters:

```
00:00 Introduction
01:02 The Scenario
01:18 The Words
01:43 Act One — Versioning: an overwrite does not destroy the old data
02:24 Act Two — Object Lock: nobody can delete a locked version
03:07 Act Three — The backup job gets an identity that can only add objects
03:47 Act Four — A checksum catches a damaged backup
04:15 Act Five — Keeping history without keeping it for ever, and the cost
04:59 Three Protections
05:24 Who Does What
05:51 Where You Have Seen It
06:16 How To Use It
06:39 Thanks for Watching
```

## Tags

backups, ransomware, object lock, s3, rustfs, versioning, least privilege, docker, java, cybersecurity for beginners

## Files

| What | File |
| --- | --- |
| Video | `video/backups-and-recovery-with-rustfs-explained.mp4` |
| Captions | `video/backups-and-recovery-with-rustfs-explained.srt` |
| Thumbnail | `docs/thumbnail.png` (1280×720) |

## Settings

Public with instant Premiere; not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Security Foundations in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — FIX (106)
- [ ] tags are at most 500 characters in total — ok (107)
- [ ] description is at most 5,000 characters — ok (648)
- [ ] chapters start at 00:00 and there are at least three
