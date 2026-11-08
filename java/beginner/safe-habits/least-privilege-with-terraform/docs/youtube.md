# YouTube sheet — Least Privilege with Terraform and LocalStack

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Least Privilege with Terraform and LocalStack (IAM Policies, Blast Radius, Boundaries)

## Description

Least privilege with Terraform and LocalStack, in Java: cloud permissions are code, so they can be checked like code. Explained with an online store, like a key cabinet where the nightly export gets a key to one drawer and not the master key. We write AWS IAM policies in Terraform, apply them with OpenTofu to a local AWS emulator in Docker, read the same policies back in Java and count the blast radius of each, see how wildcard matching and the difference between a bucket and its contents trips people up, watch an explicit deny and a permissions boundary narrow an over-broad policy, and run a linter that catches iam PassRole on every role. Everything runs on your own machine, with no cloud account.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/beginner/safe-habits/least-privilege-with-terraform

Chapters:

```
00:00 Introduction
01:03 The Scenario
01:26 The Words
01:52 What the Letters Stand For
02:59 What the Letters Stand For (continued)
03:08 How a request is decided
04:10 25 things a stolen key could do, before and after
05:24 Act One — Policies written as code
05:54 Six requests against the exporter's policy
07:07 Act Two — How a request is matched
07:25 Act Two — How a request is matched
07:45 Two statements disagree: the deny wins
08:46 Act Three — A deny beats every allow
09:09 A boundary is a ceiling: only the overlap is allowed
10:10 Act Four — A permissions boundary
10:30 Act Four — A permissions boundary
10:46 3 of 25 that can become everything
11:47 Act Five — A linter, and the way to more power
12:03 Act Five — A linter, and the way to more power
12:26 From Wide to Narrow
12:52 Where You Have Seen It
13:16 How To Use It
13:43 The Java Classes
13:55 PolicyDocument — a statement: effect, actions, resources
14:18 Glob — a star matches any run of letters
14:36 Evaluator — a deny wins at once
14:53 Evaluator — both must allow
15:05 Catalogue — count what a key could do
15:19 Linter — a way to more power
15:39 Thanks for Watching
```

## Tags

least privilege, terraform, opentofu, localstack, iam policy, permissions boundary, blast radius, java, cybersecurity for beginners, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/least-privilege-with-terraform-explained.mp4` |
| Captions | `video/least-privilege-with-terraform-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Safe Habits in Java - With Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (86)
- [ ] tags are at most 500 characters in total — ok (174)
- [ ] description is at most 5,000 characters — ok (707)
- [ ] chapters start at 00:00 and there are at least three
