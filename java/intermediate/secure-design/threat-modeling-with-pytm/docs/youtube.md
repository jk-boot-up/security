# YouTube sheet — Threat Modeling with pytm

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Threat Modeling with pytm: 113 Threats, Answers That Change Findings, Diagrams, Reports (OWASP A06)

## Description

The bank's data-flow design (a customer, a mobile app, an API gateway, a payments service, two stores and a card processor across four trust boundaries) written as a pytm 1.4 model by a Java program: the 113 threats of pytm's library checked against every element and flow, 170 findings when nothing is said, 167, 159, 111 and 94 as encryption, authentication, access control and input handling, and resilience are declared, a diagram drawn by Graphviz, a report from a template, a draft with a flow that skips a boundary that pytm accepts, and a refund flow over plain http that adds findings.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/intermediate/secure-design/threat-modeling-with-pytm

Chapters:

```
00:00 Introduction
00:56 The Scenario
01:28 The Words
02:15 What the Letters Stand For
03:16 What the Letters Stand For (continued)
04:08 What the Letters Stand For (continued)
04:45 One threat, one element, one answer
05:48 The bank as pytm sees it
06:52 Act One — The bank, written for pytm
07:08 Act One — The bank, written for pytm
07:23 Act One — The bank, written for pytm
07:41 Act One — The bank, written for pytm
08:01 Act One — The bank, written for pytm
08:24 170 findings, out of the threats for each kind
09:19 Act Two — What pytm finds when nothing is said
09:41 Act Two — What pytm finds when nothing is said
10:00 Act Two — What pytm finds when nothing is said
10:18 More answers, fewer findings
11:15 Act Three — Every answer changes the findings
11:28 Act Three — Every answer changes the findings
11:47 Act Three — Every answer changes the findings
12:06 Act Three — Every answer changes the findings
12:25 One model, three outputs
13:10 Act Four — A diagram and a report
13:32 Act Four — A diagram and a report
13:47 A change, and a finding that does not apply
14:51 Act Five — The bill
15:11 Act Five — The bill
15:37 What It Costs
16:13 Where You Have Seen It
16:43 How To Use It
17:16 The Java Classes
17:31 Design — one element, one flow
17:50 Answers — a stage adds control lines
18:08 Writer — one line per element and per flow
18:24 Pytm — the findings of one model
18:46 Condition — or, and, not, as in Python
19:07 Thanks for Watching
```

## Tags

pytm, threat modeling, stride, capec, data flow diagram, graphviz, secure design, owasp top 10, a06, java, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/threat-modeling-with-pytm-explained.mp4` |
| Captions | `video/threat-modeling-with-pytm-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Secure Design in Java - With Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (99)
- [ ] tags are at most 500 characters in total — ok (147)
- [ ] description is at most 5,000 characters — ok (594)
- [ ] chapters start at 00:00 and there are at least three
