# YouTube sheet — Detecting Attacks in Logs

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Detecting Attacks in Logs: Rules, Thresholds and False Positives (Java)

## Description

Detecting attacks in a bank's sign-in and admin logs, in Java: five detection rules (brute force, password spraying, impossible travel, success after failures, privilege change) run over one synthetic Monday with four hidden attacks, measured against ground truth: a first version raises 29 alerts, most false; tuning thresholds and adding context such as the VPN's addresses brings it to 9 alerts, 7 real, with every attack noticed.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/intermediate/detection-and-response/detecting-attacks

Chapters:

```
00:00 Introduction
01:08 The Scenario
01:43 The Words
02:21 What the Letters Stand For
03:11 One rule, two accounts
04:09 The Monday: four attacks among the look-alikes
05:21 Act One — One Monday of logs
05:45 Act One — One Monday of logs
06:09 Per account or per address, and a speed worked out
07:32 Act Two — Two attacks, worked out
08:04 Act Two — Two attacks, worked out
08:36 29 alerts: true in green, false in amber
09:40 Act Three — A first version of each rule
10:09 Act Three — A first version of each rule
10:22 Act Three — A first version of each rule
10:44 The normal day, the attack, and the range between them
11:46 Act Four — Tuning one threshold
12:14 Act Four — Tuning one threshold
12:38 Nine alerts on the day's clock
13:44 Act Five — The tuned set, and what it tells the analyst
14:02 Act Five — The tuned set, and what it tells the analyst
14:14 Act Five — The tuned set, and what it tells the analyst
14:30 What It Costs
15:07 Where You Have Seen It
15:45 How To Use It
16:21 The Java Classes
16:34 Rule — the shape every rule has
16:49 BruteForceRule — a window per account
17:06 SprayingRule — a window per address, counting accounts
17:21 ImpossibleTravelRule — distance over time
17:39 PrivilegeChangeRule — context, not a threshold
17:55 Peaks — the most failures each account reached
18:14 Alert — true when its evidence holds an attack event
18:29 Thanks for Watching
```

## Tags

detection engineering, password spraying, impossible travel, siem, false positives, java, application security, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/detecting-attacks-explained.mp4` |
| Captions | `video/detecting-attacks-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Detection and Response in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (71)
- [ ] tags are at most 500 characters in total — ok (155)
- [ ] description is at most 5,000 characters — ok (433)
- [ ] chapters start at 00:00 and there are at least three
