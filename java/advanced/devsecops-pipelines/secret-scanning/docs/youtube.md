# YouTube sheet — Secret Scanning

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Secret Scanning: Gitleaks, Git History, Pre-Commit and Pre-Receive Hooks

## Description

Secret scanning for a bank's git repository: a scanner written in Java next to gitleaks, files now versus the whole history, look-alikes and misses, an allowlist and a baseline, a pre-commit hook, a pre-receive hook and why a removed key must still be revoked.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/advanced/devsecops-pipelines/secret-scanning

Chapters:

```
00:00 Introduction
00:48 The Scenario
01:18 The Words
01:59 What the Letters Stand For
02:56 What the Letters Stand For (continued)
03:18 A removed key, still in the history
04:12 Entropy, worked out for one key
05:21 Act One — One string, measured by hand
05:52 Act One — One string, measured by hand
06:08 Nine strings across five commits
06:54 Act Two — The files now, and the whole history
07:03 Act Two — The files now, and the whole history
07:20 Act Two — The files now, and the whole history
07:33 Nine strings, as each scanner sees them
08:20 Act Three — What each scanner gets wrong
08:35 Act Three — What each scanner gets wrong
08:59 Known findings set aside, a new one still fails
09:48 Act Four — An allowlist and a baseline
10:01 Act Four — An allowlist and a baseline
10:16 A key stopped on the laptop, and again on the server
10:56 Act Five — Blocking a secret before it is shared
11:19 What It Costs
11:50 Who Does What
12:30 Where You Have Seen It
13:00 How To Use It
13:32 The Java Classes
13:45 Planted — a value, and whether it is real
13:59 Detector — pattern rules
14:14 Detector — entropy, and every line ever added
14:30 EntropySteps — k characters seen c times in n
14:43 Gitleaks — the download is checked
15:00 Score — compare findings with what was planted
15:12 Thanks for Watching
```

## Tags

secret scanning, gitleaks, git history, pre-commit hook, pre-receive hook, leaked credentials, devsecops, shift left, entropy, java, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/secret-scanning-explained.mp4` |
| Captions | `video/secret-scanning-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: DevSecOps Pipelines in Java - With Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (72)
- [ ] tags are at most 500 characters in total — ok (173)
- [ ] description is at most 5,000 characters — ok (260)
- [ ] chapters start at 00:00 and there are at least three
