# YouTube sheet — Passkeys with Spring Security

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Passkeys with Spring Security 7: WebAuthn Registration, Sign-In, User Verification, Clone Detection

## Description

Passkeys with Spring Security 7.1 and WebAuthn4J in a bank: registration with a challenge, a relying party id and a COSE public key the bank keeps, sign-in with a fresh challenge that is signed once, a look-alike site whose origin and relying party id the browser and the bank refuse, the defaults that accept a touch without a fingerprint and a cloned key, a wrapper that requires user verification and suspends a passkey whose counter goes backwards, synced passkeys with a counter of zero, a lost phone, removing its passkey, and switching passwords off for customers with two passkeys.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/intermediate/identity-and-access/passkeys-with-spring-security

Chapters:

```
00:00 Introduction
00:52 The Scenario
01:36 The Words
02:12 What the Letters Stand For
03:15 What the Letters Stand For (continued)
04:13 What the Letters Stand For (continued)
04:46 What the library checks, and what it leaves
05:46 Registering: the options sent, and the COSE key kept
06:52 Act One — Registering a passkey: the bank keeps only a public key
07:14 Act One — Registering a passkey: the bank keeps only a public key
07:35 How the signature is made, byte by byte
08:47 Act Two — Signing in: a fresh challenge, signed once
09:09 Act Two — Signing in: a fresh challenge, signed once
09:38 The site rule and the signed origin, worked out
10:37 Act Three — A look-alike site gets nothing it can use
10:52 Act Three — A look-alike site gets nothing it can use
11:17 The defaults, and a bank that insists
12:18 Act Four — User verification, and counters against copies
12:42 Act Four — User verification, and counters against copies
13:07 Every way in, and the password beside the passkeys
14:02 Act Five — The bill: recovery, lost devices and the move away from passwords
14:21 Act Five — The bill: recovery, lost devices and the move away from passwords
14:43 What It Costs
15:17 Where You Have Seen It
15:49 How To Use It
16:26 The Java Classes
16:41 PasskeySecurity — one line for the endpoints
16:54 PasskeySecurity — the hardened mode
17:05 HardenedRelyingParty — insist, then compare
17:21 PasswordSwitchOffFilter — two passkeys, no password
17:33 SoftwareAuthenticator — flags, COSE key, signature
17:49 Thanks for Watching
```

## Tags

passkeys, webauthn, spring security, webauthn4j, fido2, phishing resistant, owasp top 10, a07, java, identity, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/passkeys-with-spring-security-explained.mp4` |
| Captions | `video/passkeys-with-spring-security-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Identity and Access in Java - With Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — FIX (102)
- [ ] tags are at most 500 characters in total — ok (151)
- [ ] description is at most 5,000 characters — ok (589)
- [ ] chapters start at 00:00 and there are at least three
