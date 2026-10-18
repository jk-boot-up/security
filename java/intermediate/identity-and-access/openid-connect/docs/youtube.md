# YouTube sheet — OpenID Connect

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

OpenID Connect in Java (ID Token, Nonce, Discovery, Back-Channel Logout) - OWASP A07

## Description

OpenID Connect in Java with only the JDK: "sign in with the bank" at an online store, like a passport checked at a hotel desk rather than a key card that only opens a door. We follow a sign-in from the store to the bank and back, look inside the ID token and see why an access token is not proof of who signed in, check the ID token the way OpenID Connect Core requires (signature with a published key, the exact issuer, the audience and azp, expiry with skew, and the nonce bound to this browser), load the provider's metadata and keys through discovery and refuse a provider posing as another, rotate signing keys, show why the customer is identified by issuer and subject and never by email, and finish with sign-out: RP-initiated logout and back-channel logout tokens.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/intermediate/identity-and-access/openid-connect

Chapters:

```
00:00 Introduction
01:04 The Scenario
01:24 The Words
01:53 Act One — OAuth gives access; OpenID Connect adds who signed in
02:53 Act Two — Checking the ID token, as OpenID Connect Core requires
03:56 Act Three — Discovery and keys
04:58 Act Four — Who signed in: issuer and subject, never the email
05:51 Act Five — Signing out, and the bill
06:55 The Checks, In Order
07:18 Who Does What
07:50 Where You Have Seen It
08:17 How To Use It
08:44 Thanks for Watching
```

## Tags

openid connect, oidc, id token, oauth 2.0, nonce, discovery, jwks, single sign-on, java, application security

## Files

| What | File |
| --- | --- |
| Video | `video/openid-connect-explained.mp4` |
| Captions | `video/openid-connect-explained.srt` |
| Thumbnail | `docs/thumbnail.png` (1280×720) |

## Settings

Public with instant Premiere; not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Security Foundations in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (84)
- [ ] tags are at most 500 characters in total — ok (100)
- [ ] description is at most 5,000 characters — ok (772)
- [ ] chapters start at 00:00 and there are at least three
