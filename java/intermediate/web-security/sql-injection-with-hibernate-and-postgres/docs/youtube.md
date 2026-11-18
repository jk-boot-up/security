# YouTube sheet — SQL Injection with Hibernate and PostgreSQL

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

SQL Injection in Hibernate: JPQL, Native Queries and PostgreSQL Least Privilege

## Description

SQL injection in Hibernate 7 against a real PostgreSQL 18, in Java. Hibernate runs the text it is given: a JPQL or native query built by pasting a customer's words into it is injectable just like plain SQL. A quote reveals a product that is not for sale, the same hole asks yes or no questions about the account table because JPQL can name any entity, and a native UNION returns every e-mail address and password hash. Explained with an online store's product search, like a waiter who lets the customer dictate the kitchen's order slip word for word. We fix it three ways with bound parameters (named parameters in JPQL, in native SQL, and the Criteria API), choose the sort key from a fixed list, and connect as a database user who can only read products, so PostgreSQL itself refuses the attack. We finish with the cost: Docker for the demo, more than one database user, and a review of every string that reaches createQuery.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/intermediate/web-security/sql-injection-with-hibernate-and-postgres

Chapters:

```
00:00 Introduction
01:04 The Scenario
01:35 The Words
02:16 What the Letters Stand For
03:28 What the Letters Stand For (continued)
03:37 Hibernate translates the text it is given
04:34 Two tables, one search, two kettles
05:37 Act One — A search that works
05:54 Act One — A search that works
06:23 Where the customer's text ends up
07:36 Act Two — Hibernate does not stop a quote
08:03 Act Two — Hibernate does not stop a quote
08:21 Act Two — Hibernate does not stop a quote
08:36 Act Two — Hibernate does not stop a quote
08:52 The query is fixed; the text fills one slot
10:00 Act Three — Parameters keep text as text
10:13 Act Three — Parameters keep text as text
10:33 Act Three — Parameters keep text as text
10:50 Two database users, and what the same mistake reaches
11:48 Act Four — Few rights limit the damage
12:19 What It Costs
13:02 Who Does What
13:43 How To Use It
14:21 The Java Classes
14:36 UnsafeSearch — string concatenation, twice
14:55 SafeSearch — :term and setParameter
15:15 SafeSearch — the Criteria API
15:29 SortColumn — a fixed list, and a default
15:43 SqlLog — a statement inspector
16:00 Shop — create user, grant select
16:17 Thanks for Watching
```

## Tags

sql injection, hibernate, jpql, postgresql, java, application security, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/sql-injection-with-hibernate-and-postgres-explained.mp4` |
| Captions | `video/sql-injection-with-hibernate-and-postgres-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Web Security in Java - With Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (79)
- [ ] tags are at most 500 characters in total — ok (116)
- [ ] description is at most 5,000 characters — ok (928)
- [ ] chapters start at 00:00 and there are at least three
