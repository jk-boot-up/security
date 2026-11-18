# YouTube sheet — Privacy Engineering

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Privacy Engineering: Minimization, Pseudonyms, Retention and Erasure in PostgreSQL

## Description

Privacy engineering for a bank in PostgreSQL: data minimization against purposes, k-anonymity and linkage with an outside list, keyed pseudonyms, a retention job with a legal hold, a data map found from the schema, an access and erasure request, and the mentions that remain in other people's records.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/advanced/governance-risk-and-compliance/privacy-engineering

Chapters:

```
00:00 Introduction
00:48 The Scenario
01:15 The Words
01:59 What the Letters Stand For
02:54 A row with no name, and an outside list
04:03 Which purpose needs which personal column
05:12 Act One — What is collected, and why
05:38 Act One — What is collected, and why
05:55 How many customers are alone in their group
07:14 Act Two — Who can be picked out
07:42 Act Two — Who can be picked out
08:12 How a pseudonym is made
09:26 Act Three — Pseudonyms for analytics
09:50 Act Three — Pseudonyms for analytics
10:19 The retention job, before and after
11:41 Act Four — Retention
12:10 Act Four — Retention
12:39 An erasure, and what still mentions the customer
13:50 Act Five — A customer's request
14:12 Act Five — A customer's request
14:34 Act Five — A customer's request
14:52 What It Costs
15:22 Where You Have Seen It
15:51 How To Use It
16:21 The Java Classes
16:37 Purposes — a column no purpose needs
16:50 Anonymity — the full values and the coarse ones
17:07 Coarse — three digits and a decade
17:22 PseudonymSteps — the whole HMAC, then its first 8 bytes
17:41 Retention — delete old transactions, unless on hold
17:59 Subject — the data map, from the foreign keys
18:14 Subject — every text column, searched
18:29 Thanks for Watching
```

## Tags

privacy engineering, data minimization, pseudonymization, k-anonymity, data retention, gdpr, right to erasure, data subject access request, postgresql, java, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/privacy-engineering-explained.mp4` |
| Captions | `video/privacy-engineering-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Governance, Risk and Compliance in Java - With Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (82)
- [ ] tags are at most 500 characters in total — ok (198)
- [ ] description is at most 5,000 characters — ok (301)
- [ ] chapters start at 00:00 and there are at least three
