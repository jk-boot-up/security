# YouTube sheet — Cross-Site Scripting: Output Encoding and CSP

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Cross-Site Scripting (XSS) in Java: Output Encoding and a Strict CSP

## Description

Cross-site scripting (XSS) explained in Java, with the defences that work: contextual output encoding and a strict Content-Security-Policy. Explained with an online store's product reviews, like a shop notice board where anyone's note could be mistaken for the shop's own sign. We see a review change the page, write an encoder for each place text goes (between tags, in an attribute, in a URL, in a script), see why filtering input is not the fix, serve a page with a fresh nonce and a strict policy that refuses any script without it, and finish with the bill: every output context needs its encoder, and the policy needs care.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/intermediate/web-security/cross-site-scripting

Chapters:

```
00:00 Introduction
01:09 The Scenario
01:29 The Words
02:02 Act One — A review that changes the page
02:39 Act Two — Encoding for the place the text goes
03:23 Act Three — Why filtering input is not the fix
04:02 Act Four — A strict Content-Security-Policy, with a fresh nonce for every page
04:47 Act Five — The bill
05:25 Fixes, Weakest First
05:58 Who Does What
06:30 Where You Have Seen It
06:59 How To Use It
07:25 Thanks for Watching
```

## Tags

xss, cross-site scripting, output encoding, content security policy, csp nonce, owasp top 10, web security, java, application security

## Files

| What | File |
| --- | --- |
| Video | `video/cross-site-scripting-explained.mp4` |
| Captions | `video/cross-site-scripting-explained.srt` |
| Thumbnail | `docs/thumbnail.png` (1280×720) |

## Settings

Public with instant Premiere; not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Security Foundations in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (68)
- [ ] tags are at most 500 characters in total — ok (126)
- [ ] description is at most 5,000 characters — ok (629)
- [ ] chapters start at 00:00 and there are at least three
