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
01:47 What the Letters Stand For
02:51 What the Letters Stand For (continued)
03:03 One field, read by the server
03:57 The page helps; only the server can refuse
04:53 Act One — Where validation belongs
05:16 Act One — Where validation belongs
05:38 Act One — Where validation belongs
06:04 A raw string goes in; a type, or the broken rule, comes out
06:55 Act Two — Say what is allowed, and parse into types
07:21 Act Two — Say what is allowed, and parse into types
07:43 Act Two — Say what is allowed, and parse into types
08:06 Three spellings, one name: compare after the canonical form
09:05 Act Three — Canonicalise before validating
09:21 Act Three — Canonicalise before validating
09:41 Act Three — Canonicalise before validating
10:00 Act Three — Canonicalise before validating
10:22 Every split of the a's is tried: the work doubles with each one
11:20 Act Four — Limits are validation too
11:48 Act Four — Limits are validation too
12:13 Valid is not the same as safe
13:00 Act Five — The bill: what validation does not do
13:22 Act Five — The bill: what validation does not do
13:40 In Order, At The Boundary
14:05 Where You Have Seen It
14:33 How To Use It
14:56 The Java Classes
15:09 Checkout — only the expected fields
15:23 Checkout — the price comes from the catalogue
15:35 Quantity — limit, check, then build
15:50 Username — hidden characters, canonical form, allow-list
16:08 DisplayName — compare by skeleton
16:20 Limits — read at most one byte more than the limit
16:33 Backtracking — try every split of the run
16:50 Rejected — the field and the rule, nothing else
17:01 Thanks for Watching
```

## Tags

input validation, owasp top 10, allow list, unicode normalization, redos, trust boundary, secure coding, java, application security, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/input-validation-explained.mp4` |
| Captions | `video/input-validation-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Secure Coding in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (65)
- [ ] tags are at most 500 characters in total — ok (174)
- [ ] description is at most 5,000 characters — ok (626)
- [ ] chapters start at 00:00 and there are at least three
