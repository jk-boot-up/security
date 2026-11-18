# YouTube sheet — Fuzzing: Finding the Inputs Nobody Tested

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Fuzzing in Java (Mutation, Coverage Guidance, Delta Debugging)

## Description

Fuzzing in Java, explained from first principles with an online store, like a new employee who presses every button on the till in every order until something breaks. The store's order-import and cart parsers pass all their example tests, yet a mutation fuzzer finds a missing-field crash, an integer overflow and a stack overflow from deep nesting. Guided by coverage, keeping every input that reaches a new branch, it then finds a gift-message bug that blind mutation never reaches in a hundred thousand runs. Each crash is shrunk to its smallest input by delta debugging and kept as a regression test, the fixed parsers survive the same fuzzer, a property check confirms that parsing and printing agree, and the bill: fuzzing finds crashes, not wrong business decisions.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/intermediate/secure-coding/fuzzing

Chapters:

```
00:00 Introduction
01:01 The Scenario
01:19 The Words
01:48 What the Letters Stand For
02:36 One run of a fuzzer
03:30 Six example tests pass; one line nobody tested crashes
04:24 Act One — The example tests pass
04:49 Act One — The example tests pass
05:10 How the inputs are made, and how a total overflows
06:19 Act Two — A mutation fuzzer
06:46 Act Two — A mutation fuzzer
07:01 Act Two — A mutation fuzzer
07:23 Act Two — A mutation fuzzer
07:32 Guidance keeps each input that matches one more letter
08:37 Act Three — Guided by coverage
08:49 Act Three — Guided by coverage
09:14 Act Three — Guided by coverage
09:32 Shrink each crash, then fix the parser
10:27 Act Four — From crash to fix
10:38 Act Four — From crash to fix
10:53 Act Four — From crash to fix
11:14 No crash, so no report
12:05 Act Five — What fuzzing does not find
12:25 From Crash To Test
12:50 Where You Have Seen It
13:16 How To Use It
13:41 The Java Classes
13:53 Fuzzer — one change: flip a bit
14:07 Fuzzer — a crash is anything but a refusal
14:19 Fuzzer — guided: keep an input that reached a new branch
14:33 Coverage — a counter per branch
14:46 CartParser — one letter, one branch
15:00 CartParser — the bug: no colon
15:14 OrderLine — the overflow, and its fix
15:28 Minimizer — remove a piece, keep it if it still crashes
15:42 Crash — type and place
15:53 Thanks for Watching
```

## Tags

fuzzing, fuzz testing, coverage guided fuzzing, delta debugging, regression tests, secure coding, java, jazzer, application security, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/fuzzing-explained.mp4` |
| Captions | `video/fuzzing-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Secure Coding in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (62)
- [ ] tags are at most 500 characters in total — ok (175)
- [ ] description is at most 5,000 characters — ok (773)
- [ ] chapters start at 00:00 and there are at least three
