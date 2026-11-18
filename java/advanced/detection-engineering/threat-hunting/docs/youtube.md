# YouTube sheet — Threat Hunting

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Threat Hunting: Hypotheses, Leads, False Leads and Turning a Hunt into a Rule

## Description

Threat hunting at a bank on a simulated week of logs: hypotheses about beaconing, rare process pairs, odd logins and bulk volume, the leads each one gives, the false leads that teach an allowlist, a data gap no hunt can close, and turning a hunt into a rule.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/advanced/detection-engineering/threat-hunting

Chapters:

```
00:00 Introduction
00:45 The Scenario
01:20 The Words
01:58 What the Letters Stand For
02:28 From a guess to one lead
03:21 The week: the records, the truth, the look-alikes
04:21 Act One — The week
04:39 Act One — The week
04:54 How even are the gaps? Four pairs, worked out
05:49 Act Two — Hunt 1: a timer in the lookups
05:59 Act Two — Hunt 1: a timer in the lookups
06:27 Act Two — Hunt 1: a timer in the lookups
06:45 Rare program pairs, and an account's usual hours
07:47 Act Three — Hunts 2 and 3: pairs and accounts
07:57 Act Three — Hunts 2 and 3: pairs and accounts
08:10 Act Three — Hunts 2 and 3: pairs and accounts
08:26 Each host against its own normal, and the scoreboard
09:26 Act Four — Hunt 4 and the scoreboard
09:40 Act Four — Hunt 4 and the scoreboard
10:00 The beacon hunt as a rule, before and after the allowlist
10:41 Act Five — From a hunt to a rule
11:10 What It Costs
11:45 Where You Have Seen It
12:13 How To Use It
12:48 The Java Classes
12:58 Generator — a beacon with jitter, and a heartbeat without
13:15 Hunts — how even the gaps are
13:28 Hunts — regular AND rare
13:42 Hunts — each host against its own normal
13:56 ThreatHuntingDemo — the truth, and the judgement
14:06 Thanks for Watching
```

## Tags

threat hunting, hypothesis-driven hunting, beaconing detection, long tail analysis, stacking, anomaly detection, detection engineering, mitre att&ck, soc, java, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/threat-hunting-explained.mp4` |
| Captions | `video/threat-hunting-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Detection Engineering in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (77)
- [ ] tags are at most 500 characters in total — ok (201)
- [ ] description is at most 5,000 characters — ok (258)
- [ ] chapters start at 00:00 and there are at least three
