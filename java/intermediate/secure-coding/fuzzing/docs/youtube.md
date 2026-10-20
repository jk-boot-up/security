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
01:48 Act One — The example tests pass
02:25 Act Two — A mutation fuzzer
03:20 Act Three — Guided by coverage
04:11 Act Four — From crash to fix
05:06 Act Five — What fuzzing does not find
05:45 From Crash To Test
06:11 Who Does What
06:37 Where You Have Seen It
07:02 How To Use It
07:27 Thanks for Watching
```

## Tags

fuzzing, fuzz testing, coverage guided fuzzing, delta debugging, regression tests, secure coding, java, jazzer, application security

## Files

| What | File |
| --- | --- |
| Video | `video/fuzzing-explained.mp4` |
| Captions | `video/fuzzing-explained.srt` |
| Thumbnail | `docs/thumbnail.png` (1280×720) |

## Settings

Public with instant Premiere; not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Security Foundations in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (62)
- [ ] tags are at most 500 characters in total — ok (124)
- [ ] description is at most 5,000 characters — ok (773)
- [ ] chapters start at 00:00 and there are at least three
