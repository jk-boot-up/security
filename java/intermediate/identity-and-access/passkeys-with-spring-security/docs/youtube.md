# YouTube sheet — Passkeys with Spring Security

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Passkeys with Spring Security 7: WebAuthn Registration, Sign-In, User Verification and Clone Detection

## Description

Passkeys with Spring Security 7.1 and WebAuthn4J in a bank: registration with a challenge, a relying party id and a COSE public key the bank keeps, sign-in with a fresh challenge that is signed once, a look-alike site whose origin and relying party id the browser and the bank refuse, the defaults that accept a touch without a fingerprint and a cloned key, a wrapper that requires user verification and suspends a passkey whose counter goes backwards, synced passkeys with a counter of zero, a lost phone, removing its passkey, and switching passwords off for customers with two passkeys.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/intermediate/identity-and-access/passkeys-with-spring-security

Chapters:

```
00:00 Introduction
01:35 The Scenario
01:58 Act One — Registering a passkey: the bank keeps only a public key
02:28 Act Two — Signing in: a fresh challenge, signed once
02:57 Act Three — A look-alike site gets nothing it can use
03:30 Act Four — User verification, and counters against copies
04:05 Act Five — The bill: recovery, lost devices and the move away from passwords
04:37 What It Costs
05:06 Who Does What
05:35 Where You Have Seen It
05:49 How To Use It
06:12 Thanks for Watching
```

## Tags

passkeys, webauthn, spring security, webauthn4j, fido2, phishing resistant, owasp top 10, a07, java, identity

## Files

| What | File |
| --- | --- |
| Video | `video/passkeys-with-spring-security-explained.mp4` |
| Captions | `video/passkeys-with-spring-security-explained.srt` |
| Thumbnail | `docs/thumbnail.png` (1280×720) |

## Settings

Public with instant Premiere; not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Security Foundations in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — FIX (102)
- [ ] tags are at most 500 characters in total — ok (100)
- [ ] description is at most 5,000 characters — ok (589)
- [ ] chapters start at 00:00 and there are at least three
