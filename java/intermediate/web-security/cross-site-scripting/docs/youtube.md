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
01:10 The Scenario
01:30 The Words
02:03 What the Letters Stand For
02:58 What the Letters Stand For (continued)
03:24 The same text, pasted or encoded
04:20 What the browser finds when a review is pasted in
05:13 Act One — A review that changes the page
05:36 Act One — A review that changes the page
05:50 Four places, four encoders
06:53 Act Two — Encoding for the place the text goes
07:06 Act Two — Encoding for the place the text goes
07:26 Act Two — Encoding for the place the text goes
07:47 A filter deletes too much, and misses too much
08:44 Act Three — Why filtering input is not the fix
09:03 Act Three — Why filtering input is not the fix
09:20 Only the script that carries this page's nonce runs
10:21 Act Four — A strict Content-Security-Policy, with a fresh nonce
10:44 Act Four — A strict Content-Security-Policy, with a fresh nonce
11:07 What It Costs
11:52 Fixes, Weakest First
12:25 Where You Have Seen It
12:53 How To Use It
13:20 The Java Classes
13:35 ReviewPage — pasted as it is
13:47 ReviewPage — encoded for each place
14:00 Encode — between tags and in attributes
14:16 Encode — in a URL and in a script
14:31 NaiveFilter — delete two characters
14:43 ContentSecurityPolicy — nonce and policy
14:54 ContentSecurityPolicy — which inline script may run
15:08 ShopServer — a new nonce for every response
15:21 Thanks for Watching
```

## Tags

xss, cross-site scripting, output encoding, content security policy, csp nonce, owasp top 10, web security, java, application security, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/cross-site-scripting-explained.mp4` |
| Captions | `video/cross-site-scripting-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Web Security in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (68)
- [ ] tags are at most 500 characters in total — ok (177)
- [ ] description is at most 5,000 characters — ok (629)
- [ ] chapters start at 00:00 and there are at least three
