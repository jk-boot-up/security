# YouTube sheet — The Principle of Least Privilege

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

The Principle of Least Privilege in Java (Roles, Blast Radius, Expiring Access)

## Description

The principle of least privilege in Java: give people and programs only the access they need. Explained with an online store, like hotel keys that open one room and expire at checkout, not a master key for every guest. We model roles and permissions with deny by default, measure the blast radius of a stolen credential for an admin and for a scoped job, require two different people for a refund, give access that expires on its own, find permissions nobody uses, and lock a file to its owner with operating system permissions.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/beginner/safe-habits/least-privilege

Chapters:

```
00:00 Introduction
00:59 The Scenario
01:19 The Words
01:47 What the Letters Stand For
02:46 Who can do what in the store
03:49 How a request is decided, statement by statement
05:07 Act One — Roles and permissions, and deny by default
05:30 Act One — Roles and permissions, and deny by default
05:53 The blast radius of one stolen token
06:58 Act Two — Blast radius: what a stolen credential can reach
07:30 Two people for a refund: the requests and the results
08:42 Act Three — Separation of duties: two people for a refund
09:07 A grant that ends by itself
10:06 Act Four — Access that expires by itself
10:24 Unused permissions, and owner-only files
11:45 Act Five — Privilege creep, and the operating system
12:01 Act Five — Privilege creep, and the operating system
12:27 Ways To Shrink Privilege
12:47 Where You Have Seen It
13:17 How To Use It
13:41 The Java Classes
13:53 Policy — deny by default, and a deny wins
14:14 Catalogue — roles are bundles of permissions
14:32 Access — a grant only counts inside its window
14:51 RefundDesk — permission first, then not the creator
15:07 UsageAudit — never used, or too long ago
15:22 OwnerOnly — lock a file to its owner
15:37 Thanks for Watching
```

## Tags

least privilege, rbac, blast radius, separation of duties, deny by default, access control, java, cybersecurity for beginners, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/least-privilege-explained.mp4` |
| Captions | `video/least-privilege-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Safe Habits in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (79)
- [ ] tags are at most 500 characters in total — ok (169)
- [ ] description is at most 5,000 characters — ok (528)
- [ ] chapters start at 00:00 and there are at least three
