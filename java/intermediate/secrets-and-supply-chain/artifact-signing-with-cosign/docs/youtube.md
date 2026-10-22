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
01:32 The Scenario
01:53 Act One — A deployment that runs whatever a tag points at
02:15 Act Two — A signed release, checked before it runs
02:41 Act Three — Signed claims, and keys that rotate
03:08 Act Four — Signed statements about how the image was built
03:37 Act Five — The bill
03:59 What It Costs
04:24 Who Does What
04:38 Where You Have Seen It
04:57 How To Use It
05:19 Thanks for Watching
```

## Tags

cosign, sigstore, container signing, slsa, attestation, supply chain, owasp top 10, a08, docker, java

## Files

| What | File |
| --- | --- |
| Video | `video/artifact-signing-with-cosign-explained.mp4` |
| Captions | `video/artifact-signing-with-cosign-explained.srt` |
| Thumbnail | `docs/thumbnail.png` (1280×720) |

## Settings

Public with instant Premiere; not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Security Foundations in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (92)
- [ ] tags are at most 500 characters in total — ok (92)
- [ ] description is at most 5,000 characters — ok (585)
- [ ] chapters start at 00:00 and there are at least three
