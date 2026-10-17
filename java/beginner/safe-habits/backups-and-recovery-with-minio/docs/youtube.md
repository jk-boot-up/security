# YouTube sheet — Backups and Recovery with an S3 Object Store

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Backups and Recovery with an S3 Object Store (Versioning, Object Lock, Write-Only Keys)

## Description

Backups and recovery with an S3-compatible object store in Java: the same API as MinIO and Amazon S3, run locally in Docker. Explained with an online store, like a safe deposit box where the courier can post envelopes through a slot but cannot open the box. We watch ransomware overwrite a file and restore the older version, lock a backup with Object Lock so that not even the root account can delete it, give the backup job an identity that can only add objects, catch a damaged upload with a SHA-256 checksum, and finish with the bill: storage grows with every version, a compliance lock cannot be undone, and the root account is the real key.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/beginner/safe-habits/backups-and-recovery-with-minio

Chapters:

```
00:00 Introduction
00:59 The Scenario
01:16 The Words
01:41 Act One — Versioning: an overwrite does not destroy the old data
02:22 Act Two — Object Lock: nobody can delete a locked version
03:04 Act Three — The backup job gets an identity that can only add objects
03:44 Act Four — A checksum catches a damaged backup
04:13 Act Five — Keeping history without keeping it for ever, and the cost
04:56 Three Protections
05:22 Who Does What
05:48 Where You Have Seen It
06:11 How To Use It
06:35 Thanks for Watching
```

## Tags

backups, ransomware, object lock, s3, minio, versioning, least privilege, docker, java, cybersecurity for beginners

## Files

| What | File |
| --- | --- |
| Video | `video/backups-and-recovery-with-minio-explained.mp4` |
| Captions | `video/backups-and-recovery-with-minio-explained.srt` |
| Thumbnail | `docs/thumbnail.png` (1280×720) |

## Settings

Public with instant Premiere; not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Security Foundations in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (87)
- [ ] tags are at most 500 characters in total — ok (106)
- [ ] description is at most 5,000 characters — ok (646)
- [ ] chapters start at 00:00 and there are at least three
