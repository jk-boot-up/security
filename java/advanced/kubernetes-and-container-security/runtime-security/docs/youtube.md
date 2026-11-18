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
00:48 The Scenario
01:21 The Words
01:56 What the Letters Stand For
02:52 What the Letters Stand For (continued)
03:09 How one audit record is read
04:07 Valid rules, a driver that cannot load
05:01 Act One — Falco on this machine
05:15 Act One — Falco on this machine
05:29 The baseline, the work, and no alert
06:20 Act Two — Normal work, no alert
06:30 Act Two — Normal work, no alert
06:38 Act Two — Normal work, no alert
07:05 A shell where none belongs
07:47 Act Three — A shell in the container
08:02 Act Three — A shell in the container
08:12 Prevent with a read-only root, detect all the same
09:03 Act Four — A write where it never writes
09:18 Act Four — A write where it never writes
09:31 A call to an address off the baseline
10:16 Act Five — A connection it never makes
10:38 What It Costs
11:10 Where You Have Seen It
11:41 How To Use It
12:13 The Java Classes
12:27 Audit — the container, from the cgroup path
12:42 Audit — a socket address, from hex
12:55 Rules — a shell, a write, a connection
13:11 Sensor — a container that shares the kernel's processes
13:26 Falco — falco --validate
13:38 Thanks for Watching
```

## Tags

runtime security, falco, kubernetes security, container security, linux audit, ebpf, intrusion detection, kind, devsecops, java, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/runtime-security-explained.mp4` |
| Captions | `video/runtime-security-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Kubernetes and Container Security in Java - With Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (87)
- [ ] tags are at most 500 characters in total — ok (169)
- [ ] description is at most 5,000 characters — ok (268)
- [ ] chapters start at 00:00 and there are at least three
