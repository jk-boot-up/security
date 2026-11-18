# YouTube sheet — Security Requirements with OWASP ASVS

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Security Requirements with OWASP ASVS in Java (Levels, Checks, Coverage)

## Description

Security requirements with OWASP ASVS, in Java, for OWASP Top 10 A06, insecure design. Explained with an online store and an online bank, like a building inspector's checklist that the builder runs every evening instead of once at the end. A small decision model puts a brochure site at L1, the store at L2 and the bank at L3, with its reasons; fourteen requirements are written down as data, each with its chapter, level and how it is verified; ten checks ask the running store questions, and nine pass; a coverage report shows what is met, failed and still manual, and a release with the debug flag on is refused; and the bill: a standard is a starting point, not a threat model, and people still review what no test can.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/intermediate/secure-design/security-requirements-with-asvs

Chapters:

```
00:00 Introduction
00:59 The Scenario
01:17 The Words
01:52 What the Letters Stand For
02:41 What the Letters Stand For (continued)
03:17 One requirement, from a row to a verdict
04:20 Three applications, three levels
05:24 Act One — Choosing a level
05:40 Act One — Choosing a level
05:58 Fourteen requirements, as data
06:56 Act Two — Requirements written down as data
07:07 Act Two — Requirements written down as data
07:19 Act Two — Requirements written down as data
07:31 Ten questions, and the password check worked out
08:37 Act Three — Checks that ask the running store
08:47 Act Three — Checks that ask the running store
08:59 Act Three — Checks that ask the running store
09:18 Coverage at L2, and the release gate
10:21 Act Four — Coverage and gaps
10:34 Act Four — Coverage and gaps
10:45 Act Four — Coverage and gaps
11:04 The bill: people, editions, and what is not said
12:00 Act Five — The bill
12:15 Act Five — The bill
12:28 From Standard to Gate
12:52 Where You Have Seen It
13:14 How To Use It
13:37 The Java Classes
13:48 LevelChooser — each step up has a reason
14:02 Requirement — a manual requirement names no check
14:18 Checks — the password check reads the stored value
14:30 Assessment — four outcomes per requirement
14:44 Assessment — the release gate
14:57 Thanks for Watching
```

## Tags

owasp asvs, security requirements, application security verification standard, owasp top 10, a06, secure design, nist ssdf, owasp samm, java, application security, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/security-requirements-with-asvs-explained.mp4` |
| Captions | `video/security-requirements-with-asvs-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Secure Design in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (72)
- [ ] tags are at most 500 characters in total — ok (204)
- [ ] description is at most 5,000 characters — ok (723)
- [ ] chapters start at 00:00 and there are at least three
