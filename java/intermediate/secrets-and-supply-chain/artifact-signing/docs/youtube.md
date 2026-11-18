# YouTube sheet — Artifact Signing: Signed Releases, Key Rotation and Transparency Logs

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Artifact Signing in Java (Ed25519 Releases, Key Rotation, Transparency Logs)

## Description

Artifact signing in Java: an online store's deployment downloads its release jar and runs whatever arrives, so a jar with one byte changed on a mirror goes live. Then a signed release statement, with the SHA-256 of the exact bytes and an Ed25519 signature checked against a pinned key, refuses a changed jar, an edited statement and a forged key. Signing keys are kept per channel, rotated with an overlap and revoked, and a backdated signature from a stolen key shows why the signer's own date cannot be trusted. A public, append-only Merkle log, with inclusion and consistency proofs as in Certificate Transparency, makes every signature visible: the store's monitor spots a release it never made, and a log that shows two different views is caught. The bill: verify at the point of use, protect the signing key, and consider keyless signing. Mapped to OWASP Top 10 2025 A08 and A03, CWE-494 and CWE-347, and SLSA.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/intermediate/secrets-and-supply-chain/artifact-signing

Chapters:

```
00:00 Introduction
01:08 The Scenario
01:33 The Words
02:03 What the Letters Stand For
03:14 One release, signed and checked
04:21 One bit flipped on the way, and nobody checks
05:19 Act One — A deployment that runs whatever arrives
05:49 Six checks in order, and where each attempt stops
06:51 Act Two — A signed release, checked before it runs
07:22 Act Two — A signed release, checked before it runs
07:36 Act Two — A signed release, checked before it runs
07:57 Two keys, an overlap, a revocation and a backdated signature
09:02 Act Three — Signing keys: per channel, rotated, revoked
09:33 Act Three — Signing keys: per channel, rotated, revoked
09:51 How the log's root is made, and how a fork shows
10:57 Act Four — A public log that every signature must be in
11:19 Act Four — A public log that every signature must be in
11:52 Act Four — A public log that every signature must be in
12:10 What the gate guards, and what it costs
13:04 Act Five — The bill
13:35 Four Questions Before Anything Runs
14:00 Where You Have Seen It
14:27 How To Use It
14:59 The Java Classes
15:09 Statement — the bytes that are signed
15:25 SigningKey — sign the statement's bytes
15:37 Verifier — the key, the signature, the dates, the digest
15:55 KeyRing — revocation counts from a day
16:06 MerkleTree — a leaf and a node are hashed differently
16:21 DeployGate — the log's day, not the signer's
16:38 Monitor — a store key, an unknown digest
16:48 Thanks for Watching
```

## Tags

artifact signing, code signing, software supply chain, transparency log, merkle tree, ed25519, slsa, java, application security, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/artifact-signing-explained.mp4` |
| Captions | `video/artifact-signing-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Secrets and Supply Chain Security in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (76)
- [ ] tags are at most 500 characters in total — ok (170)
- [ ] description is at most 5,000 characters — ok (916)
- [ ] chapters start at 00:00 and there are at least three
