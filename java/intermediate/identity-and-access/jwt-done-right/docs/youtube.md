# YouTube sheet — JWT Done Right

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

JWT Done Right in Java (alg none, kid, aud, exp, RFC 8725) - OWASP A07

## Description

JSON Web Tokens done right, in Java with only the JDK. Explained with an online bank's accounts API, like a cinema ticket that anyone can read but only the box office can stamp. We take a bank access token apart into its three parts, show that a reader which only decodes believes a forged subject, fix the algorithm and token type on the server so that a header saying none or HS256 is refused, check expiry with a small clock skew, not-before, issuer, audience and scope, choose keys by key id from a set the API already holds and rotate them without downtime, and finish with the bill: an access token cannot be revoked before it expires, so lifetimes are minutes and refresh tokens stay on the server. Checked against the published Ed25519 example in RFC 8037.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/intermediate/identity-and-access/jwt-done-right

Chapters:

```
00:00 Introduction
01:05 The Scenario
01:25 The Words
01:47 Act One — Three parts, and decoding is not verifying
02:43 Act Two — The server chooses the algorithm, never the token
03:39 Act Three — Every claim that matters is checked
04:28 Act Four — Keys by id, from a set the API already holds
05:15 Act Five — The bill: a token cannot be taken back
06:02 The Checks, In Order
06:28 Who Does What
07:01 Where You Have Seen It
07:33 How To Use It
08:04 Thanks for Watching
```

## Tags

jwt, json web token, jws, eddsa, ed25519, rfc 8725, access token, api security, java, application security

## Files

| What | File |
| --- | --- |
| Video | `video/jwt-done-right-explained.mp4` |
| Captions | `video/jwt-done-right-explained.srt` |
| Thumbnail | `docs/thumbnail.png` (1280×720) |

## Settings

Public with instant Premiere; not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Security Foundations in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (70)
- [ ] tags are at most 500 characters in total — ok (97)
- [ ] description is at most 5,000 characters — ok (764)
- [ ] chapters start at 00:00 and there are at least three
