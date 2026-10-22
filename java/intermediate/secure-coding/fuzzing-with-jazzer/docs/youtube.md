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
01:34 The Scenario
01:49 Act One — The example tests pass
02:04 Act Two — The real Jazzer
02:21 Act Three — Guided by coverage, and by what you give it
02:43 Act Four — From crash to fix
03:00 Act Five — What fuzzing finds besides crashes, and what it does not
03:21 What It Costs
03:45 Who Does What
04:12 Where You Have Seen It
04:31 How To Use It
04:55 Thanks for Watching
```

## Tags

jazzer, fuzzing, libfuzzer, java fuzzing, coverage guided fuzzing, sanitizers, regression tests, ssdf, pw.8, java

## Files

| What | File |
| --- | --- |
| Video | `video/fuzzing-with-jazzer-explained.mp4` |
| Captions | `video/fuzzing-with-jazzer-explained.srt` |
| Thumbnail | `docs/thumbnail.png` (1280×720) |

## Settings

Public with instant Premiere; not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Security Foundations in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (97)
- [ ] tags are at most 500 characters in total — ok (104)
- [ ] description is at most 5,000 characters — ok (576)
- [ ] chapters start at 00:00 and there are at least three
