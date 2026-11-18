# YouTube sheet — OpenID Connect with Spring Security and Keycloak

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

OpenID Connect with Spring Security and Keycloak: ID Token Checks, Takeover, Back-Channel Logout

## Description

OpenID Connect login with Spring Security 7 and a real Keycloak 26 in Docker: the authorization request with state, nonce and PKCE, the ID token against the access token, what Spring's validator refuses (a key never published, a wrong issuer, a wrong audience, azp, expiry with skew, no subject, alg none), discovery that checks the issuer and not where the keys are, key rotation, an account takeover by e-mail address through a second provider, userinfo, RP-initiated logout, and real back-channel logout from Keycloak that ends one of Ana's two sessions.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/intermediate/identity-and-access/openid-connect-with-spring-security-and-keycloak

Chapters:

```
00:00 Introduction
00:57 The Scenario
01:26 The Words
01:58 What the Letters Stand For
03:05 What the Letters Stand For (continued)
04:04 What the Letters Stand For (continued)
05:05 What the Letters Stand For (continued)
05:15 One sign-in, two tokens
06:10 The request Spring Security builds, and the claims that come back
07:14 Act One — OAuth gives access; OpenID Connect adds who signed in
07:36 Act One — OAuth gives access; OpenID Connect adds who signed in
07:51 Act One — OAuth gives access; OpenID Connect adds who signed in
08:21 What the store checks every ID token against
09:26 Act Two — Checking the ID token
09:41 Act Two — Checking the ID token
09:59 Act Two — Checking the ID token
10:21 Act Two — Checking the ID token
10:43 From one issuer to the metadata, and a key rotation
11:39 Act Three — Discovery and keys
11:50 Act Three — Discovery and keys
12:13 Act Three — Discovery and keys
12:30 One e-mail address, two people
13:21 Act Four — Who signed in: issuer and subject, never the email
13:38 Act Four — Who signed in: issuer and subject, never the email
13:53 Two ways out: through the browser, and server to server
14:47 Act Five — Signing out, and the bill
15:04 Act Five — Signing out, and the bill
15:20 Act Five — Signing out, and the bill
15:44 What It Costs
16:17 Where You Have Seen It
16:50 How To Use It
17:28 The Java Classes
17:39 StoreSecurity — sign-in with PKCE, sign-out at both ends
18:01 StoreController — issuer and subject from the checked ID token
18:16 StoreAccounts — by e-mail address, and by issuer and subject
18:30 OidcSpringDemo — the checks Spring Security runs
18:49 KeycloakLab — ending a session at the provider
19:03 Thanks for Watching
```

## Tags

openid connect, spring security, keycloak, id token, nonce, back-channel logout, oauth2login, owasp top 10, a07, java, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/openid-connect-with-spring-security-and-keycloak-explained.mp4` |
| Captions | `video/openid-connect-with-spring-security-and-keycloak-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Identity and Access in Java - With Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — FIX (104)
- [ ] tags are at most 500 characters in total — ok (159)
- [ ] description is at most 5,000 characters — ok (557)
- [ ] chapters start at 00:00 and there are at least three
