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
00:55 The Scenario
01:19 The Words
01:56 What the Letters Stand For
02:52 What the Letters Stand For (continued)
03:12 Six questions asked of one flow
04:12 The bank as a data-flow model
05:18 Act One — The system as a data-flow model
05:33 Act One — The system as a data-flow model
05:45 Act One — The system as a data-flow model
06:02 The six rules on the whole model
07:01 Act Two — STRIDE per element
07:20 Act Two — STRIDE per element
07:33 Act Two — STRIDE per element
07:50 Twenty threats on the risk matrix, and the decisions
08:46 Act Three — Rating and deciding
09:04 Act Three — Rating and deciding
09:31 Eight checks on three versions of the model
10:24 Act Four — Mitigations as tests
10:48 Act Four — Mitigations as tests
10:58 Act Four — Mitigations as tests
11:08 An accepted risk expires; the four questions do not
12:04 Act Five — The bill, and the habit
12:31 The Four Questions
12:57 Where You Have Seen It
13:19 How To Use It
13:42 The Java Classes
13:55 Model — checking the picture
14:09 StrideRules — rules that follow the shape of the diagram
14:25 Rating — the weaker end sets the likelihood
14:37 Register — an accepted risk has an owner and a day
14:51 Checks — the TLS check
15:04 Thanks for Watching
```

## Tags

threat modeling, stride, data flow diagram, trust boundary, owasp top 10, a06, insecure design, secure design, java, application security, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/threat-modeling-explained.mp4` |
| Captions | `video/threat-modeling-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Secure Design in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (64)
- [ ] tags are at most 500 characters in total — ok (179)
- [ ] description is at most 5,000 characters — ok (733)
- [ ] chapters start at 00:00 and there are at least three
