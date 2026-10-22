# YouTube sheet — Input Validation with Hibernate Validator

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Input Validation with Hibernate Validator: Constraints, Groups, Custom Rules and ReDoS (OWASP A05)

## Description

Hibernate Validator 9.1 with Jakarta Validation 3.1 on a checkout form written as Java records: constraints for quantity, sku, e-mail and country; all violations reported at once with their paths; a custom constraint for invisible characters and a class-level rule that checks the postal code against the country; method validation on a service; a group sequence that checks limits before shapes before a catalogue lookup; a cart of 10000 lines and a nested pattern stopped by a length limit; and what validation does not do, including message templates that evaluate expression language.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/intermediate/secure-coding/input-validation-with-hibernate-validator

Chapters:

```
00:00 Introduction
01:32 The Scenario
01:50 Act One — Where validation belongs
02:08 Act Two — Say what is allowed, in annotations
02:29 Act Three — Your own constraints, across fields, and in an order
02:51 Act Four — Limits are validation too
03:14 Act Five — The bill: what validation does not do
03:32 What It Costs
03:54 Who Does What
04:17 Where You Have Seen It
04:36 How To Use It
05:02 Thanks for Watching
```

## Tags

hibernate validator, jakarta validation, bean validation, input validation, redos, owasp top 10, a05, cwe-20, java

## Files

| What | File |
| --- | --- |
| Video | `video/input-validation-with-hibernate-validator-explained.mp4` |
| Captions | `video/input-validation-with-hibernate-validator-explained.srt` |
| Thumbnail | `docs/thumbnail.png` (1280×720) |

## Settings

Public with instant Premiere; not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Security Foundations in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (98)
- [ ] tags are at most 500 characters in total — ok (106)
- [ ] description is at most 5,000 characters — ok (588)
- [ ] chapters start at 00:00 and there are at least three
