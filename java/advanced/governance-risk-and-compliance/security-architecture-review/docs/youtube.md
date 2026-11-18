# YouTube sheet — Security Architecture Review

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Security Architecture Review: Trust Boundaries, Data Flows and Decision Records (Structurizr, ADRs)

## Description

Security architecture review for a bank: a design in the Structurizr DSL read in Java, trust zones and flows checked by rules, STRIDE questions generated from the model, architecture decision records with their status, planted weaknesses as the truth, and what a review of a model cannot find.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/advanced/governance-risk-and-compliance/security-architecture-review

Chapters:

```
00:00 Introduction
00:45 The Scenario
01:15 The Words
02:02 What the Letters Stand For
03:02 How one rule reads one flow
04:14 The first design: parts, zones and flows
05:27 Act One — The design as drawn
05:45 Act One — The design as drawn
06:06 Act One — The design as drawn
06:26 Ten planted weaknesses, and what the first rules report
07:26 Act Two — Rules over the design
07:34 Act Two — Rules over the design
07:40 Act Two — Rules over the design
08:08 Act Two — Rules over the design
08:24 Act Two — Rules over the design
08:33 The flow inside one zone, and the score
09:37 Act Three — One more rule
10:00 Act Three — One more rule
10:16 The same five numbers, before and after the review
11:08 Act Four — The reviewed design
11:24 Act Four — The reviewed design
11:43 Act Four — The reviewed design
11:55 What the rules gave, and what is left to people
12:44 Act Five — The bill
12:59 What It Costs
13:27 Where You Have Seen It
13:57 How To Use It
14:30 The Java Classes
14:45 Design — every element becomes a part with a kind and a zone
15:03 Rules — no direct path to data, and encrypted crossings
15:19 Rules — a decision must exist and be accepted
15:32 FlowCheck — the answers decide which rules fire
15:45 Stride — letters by kind, and three for a flow
16:02 Changes — a removed flow, or a changed property
16:14 Planted — found means the same rule on the same subject
16:25 Thanks for Watching
```

## Tags

security architecture review, threat modeling, stride, structurizr, c4 model, architecture decision records, trust boundaries, data flow, design review, java, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/security-architecture-review-explained.mp4` |
| Captions | `video/security-architecture-review-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Governance, Risk and Compliance in Java - With Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (99)
- [ ] tags are at most 500 characters in total — ok (199)
- [ ] description is at most 5,000 characters — ok (293)
- [ ] chapters start at 00:00 and there are at least three
