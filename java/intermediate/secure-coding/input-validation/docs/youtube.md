# YouTube sheet — Input Validation Done Right

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Input Validation Done Right in Java (Allow-Lists, Unicode, ReDoS)

## Description

Input validation in Java, done right, for OWASP Top 10 A05 and A06. Explained with an online store, like a door attendant who checks tickets at the door instead of trusting a sticker the visitor printed at home. We send a negative quantity and a hidden price straight past the checkout page, parse every field into a type that cannot hold anything else, fold full-width and look-alike names into one canonical form, limit request bodies, carts and fields, count the steps of a regular expression that doubles its work with every character, and finish with the bill: a valid value is still not safe to put in a page or a query.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/intermediate/secure-coding/input-validation

Chapters:

```
00:00 Introduction
00:59 The Scenario
01:18 The Words
01:47 Act One — Where validation belongs
02:27 Act Two — Say what is allowed, and parse into types
03:13 Act Three — Canonicalise before validating
04:08 Act Four — Limits are validation too
04:56 Act Five — The bill: what validation does not do
05:39 In Order, At The Boundary
06:03 Who Does What
06:30 Where You Have Seen It
06:57 How To Use It
07:20 Thanks for Watching
```

## Tags

input validation, owasp top 10, allow list, unicode normalization, redos, trust boundary, secure coding, java, application security

## Files

| What | File |
| --- | --- |
| Video | `video/input-validation-explained.mp4` |
| Captions | `video/input-validation-explained.srt` |
| Thumbnail | `docs/thumbnail.png` (1280×720) |

## Settings

Public with instant Premiere; not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Security Foundations in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (65)
- [ ] tags are at most 500 characters in total — ok (123)
- [ ] description is at most 5,000 characters — ok (626)
- [ ] chapters start at 00:00 and there are at least three
