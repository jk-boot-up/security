# YouTube sheet — Cross-Site Request Forgery with Spring Security

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

CSRF in Spring Security: Tokens, SPA Cookie Mode and What Disabling CSRF Costs

## Description

Cross-site request forgery with Spring Security 7, in Java. A browser attaches a bank's session cookie to every request to the bank, even one sent by a page on another site, so a forged form can move a customer's money. Explained with an online bank, like a signed cheque that anyone could fill in, until the bank also asks for a code printed only in the customer's own cheque book. We switch the protection off to see the attack, then watch Spring Security's default token refuse it, use the cookie mode a single-page app needs, and find the three ways the defence is lost: a switch, an exemption, and a route that moves money on GET. We finish with the bill: SameSite is not set by default, and every state-changing route needs a test without a token.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/intermediate/web-security/cross-site-request-forgery-with-spring-security

Chapters:

```
00:00 Introduction
00:59 The Scenario
01:33 Act One — CSRF protection switched off
01:58 Act Two — Spring Security's default token
02:26 Act Three — A single-page app
02:50 Act Four — What the default does not cover
03:13 Act Five — The bill
03:36 What It Costs
04:16 Who Does What
04:48 Where You Have Seen It
05:24 How To Use It
06:03 Thanks for Watching
```

## Tags

csrf, spring security, spring boot, samesite, java, application security, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/cross-site-request-forgery-with-spring-security-explained.mp4` |
| Captions | `video/cross-site-request-forgery-with-spring-security-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Web Security in Java - With Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (78)
- [ ] tags are at most 500 characters in total — ok (118)
- [ ] description is at most 5,000 characters — ok (753)
- [ ] chapters start at 00:00 and there are at least three
