# YouTube sheet — Threat Modeling with pytm

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Threat Modeling with pytm: 113 Threats, Answers That Change the Findings, Diagrams and Reports (OWASP A06)

## Description

The bank's data-flow design (a customer, a mobile app, an API gateway, a payments service, two stores and a card processor across four trust boundaries) written as a pytm 1.4 model by a Java program: the 113 threats of pytm's library checked against every element and flow, 170 findings when nothing is said, 167, 159, 111 and 94 as encryption, authentication, access control and input handling, and resilience are declared, a diagram drawn by Graphviz, a report from a template, a draft with a flow that skips a boundary that pytm accepts, and a refund flow over plain http that adds findings.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/intermediate/secure-design/threat-modeling-with-pytm

Chapters:

```
00:00 Introduction
01:33 The Scenario
01:50 Act One — The bank, written for pytm
02:11 Act Two — What pytm finds when nothing is said
02:28 Act Three — Every answer changes the findings
02:49 Act Four — A diagram and a report
03:07 Act Five — The bill
03:26 What It Costs
03:50 Who Does What
04:08 Where You Have Seen It
04:29 How To Use It
04:49 Thanks for Watching
```

## Tags

pytm, threat modeling, stride, capec, data flow diagram, graphviz, secure design, owasp top 10, a06, java

## Files

| What | File |
| --- | --- |
| Video | `video/threat-modeling-with-pytm-explained.mp4` |
| Captions | `video/threat-modeling-with-pytm-explained.srt` |
| Thumbnail | `docs/thumbnail.png` (1280×720) |

## Settings

Public with instant Premiere; not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Security Foundations in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — FIX (106)
- [ ] tags are at most 500 characters in total — ok (96)
- [ ] description is at most 5,000 characters — ok (594)
- [ ] chapters start at 00:00 and there are at least three
