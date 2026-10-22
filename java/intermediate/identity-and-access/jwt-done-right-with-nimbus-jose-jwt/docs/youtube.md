# YouTube sheet — JWT Done Right with Nimbus JOSE + JWT

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

JWT Validation with Nimbus JOSE + JWT: Algorithms, Claims, JWKS Caching and Rotation (OWASP A07)

## Description

JWT validation with Nimbus JOSE + JWT 10 in Java 25, the same checks as the plain project with a standard library and a real key set endpoint: decoding versus verifying, a fixed algorithm and type against none, HS256 key confusion and the wrong type, expiry with clock skew, issuer, audience and scope, keys by id from a published set with caching and rotation, a retired key that lives on in the cache, a minimum time between refetches, jku and jwk headers that are never followed, and why a token cannot be taken back.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/intermediate/identity-and-access/jwt-done-right-with-nimbus-jose-jwt

Chapters:

```
00:00 Introduction
01:35 The Scenario
02:01 Act One — Decoding is not verifying
02:28 Act Two — The server chooses the algorithm
02:58 Act Three — Every claim that matters
03:36 Act Four — Keys by id, from one URL
04:13 Act Five — The bill: a token cannot be taken back
04:42 What It Costs
05:10 Who Does What
05:35 Where You Have Seen It
05:54 How To Use It
06:20 Thanks for Watching
```

## Tags

jwt, nimbus jose jwt, jwks, es256, token validation, key rotation, owasp top 10, a07, java, identity

## Files

| What | File |
| --- | --- |
| Video | `video/jwt-done-right-with-nimbus-jose-jwt-explained.mp4` |
| Captions | `video/jwt-done-right-with-nimbus-jose-jwt-explained.srt` |
| Thumbnail | `docs/thumbnail.png` (1280×720) |

## Settings

Public with instant Premiere; not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Security Foundations in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (96)
- [ ] tags are at most 500 characters in total — ok (91)
- [ ] description is at most 5,000 characters — ok (520)
- [ ] chapters start at 00:00 and there are at least three
