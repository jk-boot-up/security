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
01:53 What the Letters Stand For
02:49 What the Letters Stand For (continued)
03:46 What the Letters Stand For (continued)
04:47 One sign-in, two tokens
05:42 The request, and the claims that come back
06:52 Act One — OAuth gives access; OpenID Connect adds who signed in
07:19 Act One — OAuth gives access; OpenID Connect adds who signed in
07:35 Act One — OAuth gives access; OpenID Connect adds who signed in
08:00 How the ID token is made, and checked
09:07 Act Two — Checking the ID token, as OpenID Connect Core requires
09:32 Act Two — Checking the ID token, as OpenID Connect Core requires
09:49 Act Two — Checking the ID token, as OpenID Connect Core requires
10:17 Act Two — Checking the ID token, as OpenID Connect Core requires
10:44 From one issuer to the metadata and the keys
11:44 Act Three — Discovery and keys
12:00 Act Three — Discovery and keys
12:21 Act Three — Discovery and keys
12:33 Act Three — Discovery and keys
13:02 One email address, two people
13:58 Act Four — Who signed in: issuer and subject, never the email
14:14 Act Four — Who signed in: issuer and subject, never the email
14:34 A logout token ends one session
15:32 Act Five — Signing out, and the bill
15:52 Act Five — Signing out, and the bill
16:14 Act Five — Signing out, and the bill
16:40 The Checks, In Order
17:02 Where You Have Seen It
17:29 How To Use It
17:56 The Java Classes
18:11 Store — a state and a nonce per browser
18:24 Store — the account key: issuer and subject
18:42 Jws — sign the two parts, joined by a dot
18:57 IdTokenValidator — the key, then the signature
19:13 IdTokenValidator — then the claims
19:31 Discovery — the same issuer, keys at home
19:46 Provider — the logout token
20:04 Thanks for Watching
```

## Tags

openid connect, oidc, id token, oauth 2.0, nonce, discovery, jwks, single sign-on, java, application security, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/openid-connect-explained.mp4` |
| Captions | `video/openid-connect-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Identity and Access in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (84)
- [ ] tags are at most 500 characters in total — ok (151)
- [ ] description is at most 5,000 characters — ok (772)
- [ ] chapters start at 00:00 and there are at least three
