# YouTube sheet — Artifact Signing with cosign

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Signing Container Images with cosign: Digests, Claims, Attestations and Rotation (OWASP A08)

## Description

Signing container images with cosign 3 against a real registry in Docker: a tag moved to another image, a gate that resolves the tag to a digest and verifies a signature made by a pinned key, the signature stored next to the image, signed claims such as channel=stable, a beta build refused by the stable gate, key rotation with an overlap, a thief with a retired key, signed provenance statements with a rule of the gate's own, an attestation from the wrong key or for another digest, a pull that never meets the gate, and why keyless signing needs services a local lab does not have.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/intermediate/secrets-and-supply-chain/artifact-signing-with-cosign

Chapters:

```
00:00 Introduction
00:51 The Scenario
01:20 The Words
02:02 What the Letters Stand For
03:06 One release, signed by its digest
04:05 A tag is a name that can move; a digest is a checksum
04:59 Act One — A deployment that runs whatever a tag points at
05:30 What cosign signs, and what the gate checks
06:27 Act Two — A signed release, checked before it runs
06:51 Act Two — A signed release, checked before it runs
07:13 Claims the gate requires, and keys it trusts
08:12 Act Three — Signed claims, and keys that rotate
08:37 Act Three — Signed claims, and keys that rotate
09:05 Who signed, and what was signed
10:01 Act Four — Signed statements about how the image was built
10:22 Act Four — Signed statements about how the image was built
10:39 What the gate guards, and what it costs
11:28 Act Five — The bill
11:39 Act Five — The bill
12:02 What It Costs
12:37 Where You Have Seen It
13:13 How To Use It
13:43 The Java Classes
13:56 Lab — ask the registry what a tag means now
14:10 Cosign — verify a digest with one key
14:22 Cosign — cosign's errors, in the lesson's words
14:35 SignedPayload — the digest and the claims, inside the payload
14:48 CosignSigningDemo — resolve the tag, then try each trusted key
15:03 Thanks for Watching
```

## Tags

cosign, sigstore, container signing, slsa, attestation, supply chain, owasp top 10, a08, docker, java, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/artifact-signing-with-cosign-explained.mp4` |
| Captions | `video/artifact-signing-with-cosign-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Secrets and Supply Chain Security in Java - With Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (92)
- [ ] tags are at most 500 characters in total — ok (143)
- [ ] description is at most 5,000 characters — ok (585)
- [ ] chapters start at 00:00 and there are at least three
