# YouTube sheet — Fuzzing with Jazzer

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Fuzzing with Jazzer: Seeds, Crashes, Sanitizers and Regression Tests for Java Parsers (SSDF PW.8)

## Description

The real Jazzer 0.30 (libFuzzer for the JVM) fuzzing an online store's order line parser and cart parser: a fuzz target of three lines, seed inputs from the example tests, a fixed random seed, three distinct crashes in the careless order line parser, a gift-message crash in the cart parser that needs a seed with a note field, coverage points with and without seeds, each crashing input made smallest and replayed as a regression test with the Jazzer JUnit integration, a sanitizer that reports a regular expression injection in a search box, and what a fuzzer does not find.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/intermediate/secure-coding/fuzzing-with-jazzer

Chapters:

```
00:00 Introduction
00:51 The Scenario
01:22 The Words
01:52 What the Letters Stand For
02:40 What Jazzer does
03:29 Six example tests pass; one line nobody tested crashes
04:22 Act One — The example tests pass
04:42 Act One — The example tests pass
04:58 Three findings, and which missing field each one is
05:56 Act Two — The real Jazzer
06:15 Act Two — The real Jazzer
06:37 The seeds take the fuzzer past checks it cannot guess
07:39 Act Three — Guided by coverage, and by what you give it
08:02 Act Three — Guided by coverage, and by what you give it
08:19 Shorter, then plainer, then fixed
09:14 Act Four — From crash to fix
09:25 Act Four — From crash to fix
09:43 Act Four — From crash to fix
09:55 A vulnerability that is not a crash, and what is not found
10:55 Act Five — What fuzzing finds besides crashes, and what it does not
11:09 Act Five — What fuzzing finds besides crashes, and what it does not
11:31 What It Costs
12:07 Where You Have Seen It
12:38 How To Use It
13:11 The Java Classes
13:23 OrderLineCareless — hand the input to one parser
13:36 Parsers — a refusal is not a finding
13:47 Parsers — the search box, careless and fixed
14:01 JazzerRun — the command line
14:14 JazzerRun — reading a finding
14:24 Minimizer — plainer: A for a capital, 0 for a digit
14:37 OrderLine — the overflow, and its fix
14:51 Thanks for Watching
```

## Tags

jazzer, fuzzing, libfuzzer, java fuzzing, coverage guided fuzzing, sanitizers, regression tests, ssdf, pw.8, java, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/fuzzing-with-jazzer-explained.mp4` |
| Captions | `video/fuzzing-with-jazzer-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Secure Coding in Java - With Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (97)
- [ ] tags are at most 500 characters in total — ok (155)
- [ ] description is at most 5,000 characters — ok (576)
- [ ] chapters start at 00:00 and there are at least three
