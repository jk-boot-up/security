# YouTube sheet — Threat Modeling with STRIDE

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Threat Modeling with STRIDE in Java (OWASP A06, Insecure Design)

## Description

Threat modeling with STRIDE in Java, for OWASP Top 10 A06, insecure design. Explained with an online bank, like an architect who walks through a building plan asking where a burglar would get in, before the walls are built. The bank's mobile app, gateway, payments service, stores and card processor become a data-flow model with trust boundaries, checked for gaps; STRIDE rules turn it into twenty candidate threats; each one is rated and decided: mitigate, accept with an owner and a review date, transfer, or avoid; every mitigation becomes an executable check, so adding an unencrypted refund flow fails a test before release; and the bill: a model is only worth what its last update was, and an accepted risk has an expiry date.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/intermediate/secure-design/threat-modeling

Chapters:

```
00:00 Introduction
00:59 The Scenario
01:22 The Words
01:52 Act One — The system as a data-flow model
02:37 Act Two — STRIDE per element
03:18 Act Three — Rating and deciding
04:04 Act Four — Mitigations as tests
04:44 Act Five — The bill, and the habit
05:26 The Four Questions
05:52 Who Does What
06:17 Where You Have Seen It
06:39 How To Use It
07:02 Thanks for Watching
```

## Tags

threat modeling, stride, data flow diagram, trust boundary, owasp top 10, a06, insecure design, secure design, java, application security

## Files

| What | File |
| --- | --- |
| Video | `video/threat-modeling-explained.mp4` |
| Captions | `video/threat-modeling-explained.srt` |
| Thumbnail | `docs/thumbnail.png` (1280×720) |

## Settings

Public with instant Premiere; not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Security Foundations in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (64)
- [ ] tags are at most 500 characters in total — ok (128)
- [ ] description is at most 5,000 characters — ok (733)
- [ ] chapters start at 00:00 and there are at least three
