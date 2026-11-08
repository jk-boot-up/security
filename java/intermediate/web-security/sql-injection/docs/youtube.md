# YouTube sheet — SQL Injection and Its Defences

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

SQL Injection and Its Defences in Java

## Description

SQL injection and its defences, in Java. When a search page pastes a customer's words into a SQL statement, a single quote can end the text the program opened and turn the rest of the words into commands: a hidden product appears, and a UNION adds the customers' e-mail addresses to the results. Explained with an online store and its product search, like a waiter who lets the customer dictate the kitchen's order slip word for word. We watch a working search fail, then fix it with parameters, choose the sort column from a fixed list, and connect as a database user that can read only products. We finish with the cost: names cannot be parameters, and least privilege needs a separate database user for each job.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/intermediate/web-security/sql-injection

Chapters:

```
00:00 Introduction
00:59 The Scenario
01:35 The Words
02:05 What the Letters Stand For
02:53 Code and data in one string
03:37 Four products, one search, two kettles
04:26 Act One — A search that works
04:47 Where the customer's text ends up in the statement
05:48 Act Two — A quote turns text into code
06:10 Act Two — A quote turns text into code
06:22 Act Two — A quote turns text into code
06:36 The statement is fixed; the input fills one slot
07:26 Act Three — Parameters keep text as text
07:43 Act Three — Parameters keep text as text
07:56 Two database users, and what the same mistake reaches
08:43 Act Four — Few rights limit the damage
09:08 What It Costs
09:51 Where You Have Seen It
10:26 How To Use It
10:56 The Java Classes
11:10 UnsafeSearch — string concatenation
11:21 SafeSearch — a ? and a separate value
11:36 SortColumn — a fixed list, and a default
11:50 MiniDb — rights per table
12:03 Thanks for Watching
```

## Tags

sql injection, prepared statements, owasp top 10, java, application security, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/sql-injection-explained.mp4` |
| Captions | `video/sql-injection-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Web Security in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (38)
- [ ] tags are at most 500 characters in total — ok (123)
- [ ] description is at most 5,000 characters — ok (715)
- [ ] chapters start at 00:00 and there are at least three
