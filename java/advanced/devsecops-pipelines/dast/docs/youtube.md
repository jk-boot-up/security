# YouTube sheet — Dynamic Analysis

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Dynamic Analysis with OWASP ZAP: Passive, Active and Logged-In Scans

## Description

Dynamic application security testing (DAST) with OWASP ZAP for a bank's web application: a passive scan, an anonymous active scan, a logged-in active scan, the planted flaws as the truth, and a rescan of the fixed version.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/advanced/devsecops-pipelines/dast

Chapters:

```
00:00 Introduction
00:48 The Scenario
01:23 The Words
02:02 What the Letters Stand For
02:55 What the Letters Stand For (continued)
03:18 One site, three scans
04:11 One attack string, two answers
05:07 Act One — One attack, worked out by hand
05:26 Act One — One attack, worked out by hand
05:38 Nine planted flaws, page by page
06:30 Act Two — The application, and a passive scan
06:39 Act Two — The application, and a passive scan
06:51 Three requests, and the database doing a sum
07:42 Act Three — An active scan, without a login
08:08 Act Three — An active scan, without a login
08:16 With the session, the admin page too
09:02 Act Four — An active scan that is logged in
09:21 Act Four — An active scan that is logged in
09:31 Alerts by risk, before and after the fixes
10:20 Act Five — The fixed application
10:43 What It Costs
11:22 Who Does What
12:03 Where You Have Seen It
12:31 How To Use It
13:04 The Java Classes
13:16 BankApp — output, escaped only when fixed
13:30 BankApp — a query glued from a string, or bound
13:41 Flaw — raised when the alert is on its path
13:54 Probe — the same attack strings
14:14 Zap — the session cookie, and the chosen rules
14:28 Zap — a pinned image
14:35 Thanks for Watching
```

## Tags

dast, owasp zap, dynamic analysis, web application security, authenticated scan, xss, sql injection, devsecops, automation framework, java, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/dast-explained.mp4` |
| Captions | `video/dast-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: DevSecOps Pipelines in Java - With Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (68)
- [ ] tags are at most 500 characters in total — ok (180)
- [ ] description is at most 5,000 characters — ok (222)
- [ ] chapters start at 00:00 and there are at least three
