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
01:35 Act One — A search that works
01:56 Act Two — Hibernate does not stop a quote
02:24 Act Three — Parameters keep text as text
02:52 Act Four — Few rights limit the damage
03:12 What It Costs
03:55 Who Does What
04:37 How To Use It
05:15 Thanks for Watching
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
