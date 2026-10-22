# YouTube sheet — Race Conditions with PostgreSQL and Hibernate

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Race Conditions with PostgreSQL and Hibernate: Row Locks, Isolation Levels and Optimistic Locking (CWE-362)

## Description

A real PostgreSQL 18 in Docker: two withdrawals that read before either writes and pay out 160 from 100, a check constraint and a conditional update, SELECT FOR UPDATE with another session waiting in pg_stat_activity, NOWAIT, lock_timeout and SKIP LOCKED, READ COMMITTED against REPEATABLE READ and a retry, write skew that only SERIALIZABLE stops, Hibernate 7 optimistic locking with a version column, a pessimistic lock, a one-time payment key, a deadlock that PostgreSQL cancels, and a lock in the memory of one server.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/intermediate/secure-coding/race-conditions-and-business-logic-with-postgres

Chapters:

```
00:00 Introduction
01:45 The Scenario
02:03 Act One — Check, then act: two requests in the gap
02:24 Act Two — Row locks: making the check and the write one step
02:46 Act Three — Isolation levels
03:08 Act Four — Optimistic locking with Hibernate, and one-time keys
03:28 Act Five — The bill
03:48 What It Costs
04:14 Who Does What
04:29 Where You Have Seen It
04:46 How To Use It
05:10 Thanks for Watching
```

## Tags

race condition, postgresql, row lock, select for update, isolation level, write skew, optimistic locking, hibernate, cwe-362, java

## Files

| What | File |
| --- | --- |
| Video | `video/race-conditions-and-business-logic-with-postgres-explained.mp4` |
| Captions | `video/race-conditions-and-business-logic-with-postgres-explained.srt` |
| Thumbnail | `docs/thumbnail.png` (1280×720) |

## Settings

Public with instant Premiere; not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Security Foundations in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — FIX (107)
- [ ] tags are at most 500 characters in total — ok (121)
- [ ] description is at most 5,000 characters — ok (522)
- [ ] chapters start at 00:00 and there are at least three
