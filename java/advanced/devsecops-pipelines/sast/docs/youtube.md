# YouTube sheet — Static Analysis

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Static Analysis with Semgrep: Taint Rules, Rule Tests and Triage

## Description

Static analysis (SAST) with Semgrep for a bank's Java code: a pattern rule and a taint rule compared on planted flaws and safe look-alikes, what a taint rule cannot see across files, tests for rules, and triage with reasons and owners.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/advanced/devsecops-pipelines/sast

Chapters:

```
00:00 Introduction
00:52 The Scenario
01:22 The Words
02:02 What the Letters Stand For
02:54 What the Letters Stand For (continued)
03:13 One safe line, read by two rules
04:10 Input followed from the request to the query
05:05 Act One — One flaw, as a taint rule sees it
05:27 Act One — One flaw, as a taint rule sees it
05:36 The eight SQL lines, as each rule sees them
06:38 Act Two — Pattern against taint
06:48 Act Two — Pattern against taint
07:03 Act Two — Pattern against taint
07:19 Seven findings, matched with the truth
08:01 Act Three — Every flaw class with the taint rules
08:20 Act Three — Every flaw class with the taint rules
08:31 Expected against reported lines
09:25 Act Four — Tests for the rules
09:46 Act Four — Tests for the rules
10:01 From seven findings to a decision with a reason and an owner
10:48 Act Five — Triage
11:18 Act Five — Triage
11:25 What It Costs
11:59 Who Does What
12:35 Where You Have Seen It
13:06 How To Use It
13:36 The Java Classes
13:52 Marker — // VULN or // SAFE, and an id
14:05 Score — compare findings with markers
14:17 Score — recall and precision
14:26 TaintSteps — assigned from a tainted value, tainted
14:42 Semgrep — a scan, with --error for the build
14:53 Semgrep — expected against reported lines
15:05 Suppressions — the shape of a good one
15:18 Thanks for Watching
```

## Tags

static analysis, sast, semgrep, taint analysis, sql injection, custom rules, triage, false positives, devsecops, java, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/sast-explained.mp4` |
| Captions | `video/sast-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: DevSecOps Pipelines in Java - With Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (64)
- [ ] tags are at most 500 characters in total — ok (159)
- [ ] description is at most 5,000 characters — ok (235)
- [ ] chapters start at 00:00 and there are at least three
