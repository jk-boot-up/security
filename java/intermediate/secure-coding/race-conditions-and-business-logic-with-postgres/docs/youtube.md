# YouTube sheet — Race Conditions with PostgreSQL and Hibernate

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Race Conditions with PostgreSQL and Hibernate: Row Locks, Isolation, Optimistic Locking (CWE-362)

## Description

A real PostgreSQL 18 in Docker: two withdrawals that read before either writes and pay out 160 from 100, a check constraint and a conditional update, SELECT FOR UPDATE with another session waiting in pg_stat_activity, NOWAIT, lock_timeout and SKIP LOCKED, READ COMMITTED against REPEATABLE READ and a retry, write skew that only SERIALIZABLE stops, Hibernate 7 optimistic locking with a version column, a pessimistic lock, a one-time payment key, a deadlock that PostgreSQL cancels, and a lock in the memory of one server.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/intermediate/secure-coding/race-conditions-and-business-logic-with-postgres

Chapters:

```
00:00 Introduction
00:50 The Scenario
01:25 The Words
02:11 What the Letters Stand For
03:07 Four ways the database closes the gap
04:13 Two withdrawals in a real database, and two fixes
05:24 Act One — Check, then act: two requests in the gap
05:47 Act One — Check, then act: two requests in the gap
06:12 Act One — Check, then act: two requests in the gap
06:27 A row lock: B waits, then sees what A wrote
07:28 Act Two — Row locks: making the check and the write one step
08:00 Isolation levels, and write skew over two rows
09:13 Act Three — Isolation levels
09:37 Act Three — Isolation levels
09:59 A version number, and a payment key
11:05 Act Four — Optimistic locking with Hibernate, and one-time keys
11:33 Act Four — Optimistic locking with Hibernate, and one-time keys
11:55 A deadlock, and what each error code asks
13:06 Act Five — The bill
13:30 Act Five — The bill
13:50 What It Costs
14:21 Where You Have Seen It
14:52 How To Use It
15:24 The Java Classes
15:36 Db — the rule lives in the table
15:48 Db — sessions that wait for each other
16:01 RaceConditionsDemo — the check inside the update
16:14 RaceConditionsDemo — retry only what timing caused
16:27 HibAccount — @Version
16:42 SqlStates — codes, names and what to do
16:56 Thanks for Watching
```

## Tags

race condition, postgresql, row lock, select for update, isolation level, write skew, optimistic locking, hibernate, cwe-362, java, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/race-conditions-and-business-logic-with-postgres-explained.mp4` |
| Captions | `video/race-conditions-and-business-logic-with-postgres-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Secure Coding in Java - With Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — FIX (107)
- [ ] tags are at most 500 characters in total — ok (172)
- [ ] description is at most 5,000 characters — ok (522)
- [ ] chapters start at 00:00 and there are at least three
