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
00:53 The Scenario
01:29 What the Letters Stand For
02:38 What the Letters Stand For (continued)
03:40 What the Letters Stand For (continued)
04:22 One real token, three parts
05:22 ES256: a hash, then a signature with a random number
06:36 Act One — The bank signs a token with Nimbus
06:52 Act One — The bank signs a token with Nimbus
07:06 Act One — The bank signs a token with Nimbus
07:15 Act One — The bank signs a token with Nimbus
07:25 Ana's signature on mallory's claims
08:19 Act Two — Parsing is not verifying
08:39 Four headers against the API's own settings
09:49 Act Three — The server chooses the algorithm, never the token
09:59 Act Three — The server chooses the algorithm, never the token
10:08 The clock, and one changed claim at a time
11:14 Act Four — Every claim that matters is checked
11:29 Act Four — Every claim that matters is checked
11:37 One URL, a cache, and a retired key
12:52 Act Five — Keys by id, from the one URL in the settings
13:03 Act Five — Keys by id, from the one URL in the settings
13:13 Act Five — Keys by id, from the one URL in the settings
13:27 After the bank revokes, until the token expires
14:20 Act Six — The bill: a token cannot be taken back
14:46 What It Costs
15:23 Where You Have Seen It
15:59 How To Use It
16:40 The Java Classes
16:53 AccessTokenValidator — keys from one URL, cached
17:06 AccessTokenValidator — the algorithm and the type, fixed
17:20 AccessTokenValidator — the claims: the library's, then ours
17:37 BankIssuer — an ES256 header with a key id
17:49 JwksServer — public halves only, and a counter
18:02 DemoKeys — a fixed seed instead of random bytes
18:13 Thanks for Watching
```

## Tags

jwt, nimbus jose jwt, jwks, es256, token validation, key rotation, owasp top 10, a07, java, identity, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/jwt-done-right-with-nimbus-jose-jwt-explained.mp4` |
| Captions | `video/jwt-done-right-with-nimbus-jose-jwt-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Identity and Access in Java - With Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (96)
- [ ] tags are at most 500 characters in total — ok (142)
- [ ] description is at most 5,000 characters — ok (520)
- [ ] chapters start at 00:00 and there are at least three
