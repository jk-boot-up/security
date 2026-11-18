# YouTube sheet — SIEM Basics

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

SIEM Basics: Parsing, Normalizing, Time and Correlating Across Sources

## Description

SIEM basics for a bank: one web, database and syslog line each turned into one common event shape, dead letters for what does not parse, one query on the common fields, the time zone offset worked out and the alert it decides, a brute-force rule that only sees the attack across sources, and two alerts grouped into one case.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/advanced/detection-engineering/siem-basics

Chapters:

```
00:00 Introduction
00:53 The Scenario
01:28 The Words
02:09 What the Letters Stand For
02:55 Three lines, one shape
03:58 Lines in, events out, and what did not fit
04:58 Act One — Three formats, one shape
05:13 Act One — Three formats, one shape
05:26 Act One — Three formats, one shape
05:37 The same question, before and after normalizing
06:39 Act Two — One query on the common fields
07:01 One offset, two clocks, one alert
08:07 Act Three — The clock decides an alert
08:43 Eight here, six there: fourteen together
09:49 Act Four — Correlating across sources
10:22 Two alerts, one case
11:10 Act Five — From alerts to cases
11:26 What It Costs
12:02 Where You Have Seen It
12:36 How To Use It
13:10 The Java Classes
13:23 Parsers — a dead letter
13:32 Parsers — the clock, honoured or ignored
13:48 Event — eight fields, time in UTC
14:03 Correlator — failures before a success, all sources joined
14:18 Correlator — alerts to cases
14:25 Sources — the attack, split across two sources
14:36 Thanks for Watching
```

## Tags

siem, log normalization, log correlation, syslog, time zones, dead letter queue, security monitoring, soc, detection engineering, java, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/siem-basics-explained.mp4` |
| Captions | `video/siem-basics-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Detection Engineering in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (70)
- [ ] tags are at most 500 characters in total — ok (176)
- [ ] description is at most 5,000 characters — ok (325)
- [ ] chapters start at 00:00 and there are at least three
