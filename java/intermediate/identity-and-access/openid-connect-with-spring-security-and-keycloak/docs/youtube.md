# YouTube sheet — OpenID Connect with Spring Security and Keycloak

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

OpenID Connect with Spring Security and Keycloak: ID Token Checks, Account Takeover, Back-Channel Logout

## Description

OpenID Connect login with Spring Security 7 and a real Keycloak 26 in Docker: the authorization request with state, nonce and PKCE, the ID token against the access token, what Spring's validator refuses (a key never published, a wrong issuer, a wrong audience, azp, expiry with skew, no subject, alg none), discovery that checks the issuer and not where the keys are, key rotation, an account takeover by e-mail address through a second provider, userinfo, RP-initiated logout, and real back-channel logout from Keycloak that ends one of Ana's two sessions.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/intermediate/identity-and-access/openid-connect-with-spring-security-and-keycloak

Chapters:

```
00:00 Introduction
01:34 The Scenario
01:56 Act One — OAuth gives access; OpenID Connect adds who signed in
02:34 Act Two — Checking the ID token
03:07 Act Three — Discovery and keys
03:37 Act Four — Who signed in: issuer and subject, never the email
04:07 Act Five — Signing out, and the bill
04:41 What It Costs
05:10 Who Does What
05:36 Where You Have Seen It
05:53 How To Use It
06:17 Thanks for Watching
```

## Tags

openid connect, spring security, keycloak, id token, nonce, back-channel logout, oauth2login, owasp top 10, a07, java

## Files

| What | File |
| --- | --- |
| Video | `video/openid-connect-with-spring-security-and-keycloak-explained.mp4` |
| Captions | `video/openid-connect-with-spring-security-and-keycloak-explained.srt` |
| Thumbnail | `docs/thumbnail.png` (1280×720) |

## Settings

Public with instant Premiere; not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Security Foundations in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — FIX (104)
- [ ] tags are at most 500 characters in total — ok (108)
- [ ] description is at most 5,000 characters — ok (557)
- [ ] chapters start at 00:00 and there are at least three
