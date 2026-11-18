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
00:54 The Scenario
01:21 The Words
01:58 What the Letters Stand For
02:52 What the Letters Stand For (continued)
03:03 One rule, written on the type
03:49 The constraints travel with the type to the service
04:42 Act One — Where validation belongs
05:00 Act One — Where validation belongs
05:24 The rule on the type, the input, the result
06:16 Act Two — Say what is allowed, in annotations
06:31 Act Two — Say what is allowed, in annotations
06:47 Act Two — Say what is allowed, in annotations
07:02 Cheap checks first: a lookup only for a good shape
07:57 Act Three — Your own constraints, across fields, and in an order
08:19 Act Three — Your own constraints, across fields, and in an order
08:38 Act Three — Your own constraints, across fields, and in an order
08:51 A size limit first, so the slow pattern never runs
09:48 Act Four — Limits are validation too
10:10 Act Four — Limits are validation too
10:31 A visitor's text inside a message template
11:23 Act Five — The bill: what validation does not do
11:43 Act Five — The bill: what validation does not do
12:06 What It Costs
12:40 Where You Have Seen It
13:13 How To Use It
13:49 The Java Classes
14:02 Checkout — the rules on the type
14:16 Line — a shape, a lookup, a range
14:28 Orders — constraints on the parameter
14:39 Basic — limits, then shapes, then lookups
14:52 NoInvisible — normal form C, and no control or format characters
15:08 PostalCodeMatchesCountry — report it on the postal code
15:19 Coupon — the size first, the pattern last
15:30 UnsafeMessage — inside the template, or as a parameter
15:43 Thanks for Watching
```

## Tags

hibernate validator, jakarta validation, bean validation, input validation, redos, owasp top 10, a05, cwe-20, java, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/input-validation-with-hibernate-validator-explained.mp4` |
| Captions | `video/input-validation-with-hibernate-validator-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Secure Coding in Java - With Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (98)
- [ ] tags are at most 500 characters in total — ok (157)
- [ ] description is at most 5,000 characters — ok (588)
- [ ] chapters start at 00:00 and there are at least three
