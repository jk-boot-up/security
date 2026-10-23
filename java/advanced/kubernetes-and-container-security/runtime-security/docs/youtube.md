# YouTube sheet — Runtime Security

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Runtime Security: Detecting a Shell, a Write and a Connection in a Container with Falco

## Description

Runtime security for a bank's pods on a real kind cluster: Falco rules for a shell in a container, an unexpected write and an unexpected outbound connection, why Falco's driver cannot run on Docker Desktop, a Linux audit sensor in its place, and prevent versus detect.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/advanced/kubernetes-and-container-security/runtime-security

Chapters:

```
00:00 Introduction
01:54 The Scenario
02:21 Act One — Falco on this machine
02:35 Act Two — The service doing its work
02:45 Act Three — A shell in the container
02:52 Act Four — A write where it never writes
03:04 Act Five — A connection it never makes
03:15 Act Six — Prevent and detect
03:26 What It Costs
04:03 Who Does What
04:23 Where You Have Seen It
04:46 How To Use It
05:05 Thanks for Watching
```

## Tags

runtime security, falco, kubernetes security, container security, linux audit, ebpf, intrusion detection, kind, devsecops, java

## Files

| What | File |
| --- | --- |
| Video | `video/runtime-security-explained.mp4` |
| Captions | `video/runtime-security-explained.srt` |
| Thumbnail | `docs/thumbnail.png` (1280×720) |

## Settings

Public with instant Premiere; not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Security Foundations in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (87)
- [ ] tags are at most 500 characters in total — ok (118)
- [ ] description is at most 5,000 characters — ok (268)
- [ ] chapters start at 00:00 and there are at least three
