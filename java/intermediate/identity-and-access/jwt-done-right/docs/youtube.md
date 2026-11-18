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
01:07 The Scenario
01:27 The Words
01:49 What the Letters Stand For
02:49 What the Letters Stand For (continued)
03:41 What the Letters Stand For (continued)
04:20 One real token, three parts
05:18 From JSON to token: base64url, then the signature
06:41 Act One — How the bank builds a token
07:01 Act One — How the bank builds a token
07:19 Act One — How the bank builds a token
07:26 Ana's signature on mallory's claims
08:25 Act Two — Decoding is not verifying
08:50 Four headers against the API's own settings
09:57 Act Three — The server chooses the algorithm, never the token
10:08 Act Three — The server chooses the algorithm, never the token
10:20 The clock, and one changed claim at a time
11:32 Act Four — Every claim that matters is checked
11:51 Act Four — Every claim that matters is checked
12:01 A token names a key; the API decides which keys exist
13:03 Act Five — Keys by id, from a set the API already holds
13:15 Act Five — Keys by id, from a set the API already holds
13:29 After the bank revokes, until the token expires
14:20 Act Six — The bill: a token cannot be taken back
14:45 The Checks, In Order
15:11 Where You Have Seen It
15:44 How To Use It
16:15 The Java Classes
16:29 Policy — fixed by the server
16:47 Validator — the header against the policy, a known key
17:03 Validator — then the claims
17:15 KeySet — look up, never fetch
17:28 Base64UrlSteps — three bytes, four characters
17:45 RefreshTokens — a lookup the bank controls
17:59 Thanks for Watching
```

## Tags

jwt, json web token, jws, eddsa, ed25519, rfc 8725, access token, api security, java, application security, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/jwt-done-right-explained.mp4` |
| Captions | `video/jwt-done-right-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Identity and Access in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (70)
- [ ] tags are at most 500 characters in total — ok (148)
- [ ] description is at most 5,000 characters — ok (764)
- [ ] chapters start at 00:00 and there are at least three
