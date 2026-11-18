# YouTube sheet — Attack Trees and Abuse Cases

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Attack Trees and Abuse Cases: Which Defence Cuts the Cheapest Path? (Java)

## Description

Attack trees and abuse cases in Java for an online bank: one goal (move money out of Ana's account), fourteen attacker steps with a cost and a chance, AND and OR gates that give the cheapest path (50 EUR: change the account number) and the chance that some path works; eight priced defences, none enough on its own; a plan that cuts the cheapest path and looks again; and abuse cases written as checks that fail until the defence exists.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/intermediate/secure-design/attack-trees-and-abuse-cases

Chapters:

```
00:00 Introduction
00:50 The Scenario
01:23 The Words
02:02 What the Letters Stand For
02:54 What the Letters Stand For (continued)
03:45 Two rules, worked out on one branch
04:39 The tree: one goal, fourteen steps
05:32 Act One — The tree
05:53 Act One — The tree
06:04 Act One — The tree
06:21 Act One — The tree
06:36 Four ways in, and the cheapest
07:27 Act Two — What the tree says about the attacker
07:57 Act Two — What the tree says about the attacker
08:11 Only the defence on the cheapest path helps
09:02 Act Three — Each defence on its own
09:23 Act Three — Each defence on its own
09:36 Act Three — Each defence on its own
09:54 Cut the cheapest path, then look again
10:46 Act Four — A plan: cut the cheapest path, then look again
11:10 Act Four — A plan: cut the cheapest path, then look again
11:25 Abuse cases: red today, green when built
12:15 Act Five — Abuse cases as tests
12:30 Act Five — Abuse cases as tests
12:45 What It Costs
13:23 Where You Have Seen It
13:54 How To Use It
14:24 The Java Classes
14:36 Node and BankTree — a leaf, and a gate
14:50 Analysis — OR and AND, recursively
15:06 GateSteps — the steps, as the demo prints them
15:20 Defence and Planner — pick the best, apply it, look again
15:36 AbuseCase and BankConfig — passes only when refused
15:51 Thanks for Watching
```

## Tags

attack trees, abuse cases, threat modeling, security requirements, defence in depth, java, application security, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/attack-trees-and-abuse-cases-explained.mp4` |
| Captions | `video/attack-trees-and-abuse-cases-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Secure Design in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (74)
- [ ] tags are at most 500 characters in total — ok (156)
- [ ] description is at most 5,000 characters — ok (437)
- [ ] chapters start at 00:00 and there are at least three
