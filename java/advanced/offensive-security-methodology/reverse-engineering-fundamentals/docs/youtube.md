# YouTube sheet — Reverse Engineering Fundamentals

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Reverse Engineering Java: Constant Pool, javap, CFR and Patching a Licence Check

## Description

Reverse engineering fundamentals for a Java client: reading the constant pool and bytecode with the JDK class file API and javap, decompiling with CFR, patching a client-side licence check, and what renaming and string encoding stop and do not stop.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/advanced/offensive-security-methodology/reverse-engineering-fundamentals

Chapters:

```
00:00 Introduction
00:44 The Scenario
01:16 The Words
01:55 What the Letters Stand For
02:41 What the Letters Stand For (continued)
03:09 One class file, read four ways
04:02 The client: three class files, three keys
04:51 Act One — The program
05:10 The constant pool and the bytecode of check
06:09 Act Two — Reading it without running it
06:27 Act Two — Reading it without running it
06:37 Act Two — Reading it without running it
06:54 The source, and what the decompiler gives back
07:45 Act Three — Decompiling it
07:54 Act Three — Decompiling it
08:12 Before and after the patch
09:07 Act Four — Changing it
09:22 Act Four — Changing it
09:35 Obfuscation: what it hides, and how it comes back
10:37 Act Five — Obfuscation
10:56 Act Five — Obfuscation
11:22 What It Costs
11:53 Where You Have Seen It
12:23 How To Use It
12:49 The Java Classes
13:03 Sample — compile, then run with a key
13:20 Pool — the strings in the constant pool
13:31 Pool — a key check, found by its shape
13:46 Tools — a pinned decompiler, checked by its hash
14:03 Patcher — two instructions replace the check
14:16 XorSteps — two hex digits, one byte, XOR 0x5A
14:29 Thanks for Watching
```

## Tags

reverse engineering, java bytecode, javap, cfr decompiler, class file api, obfuscation, client-side checks, hardcoded secrets, software protection, java, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/reverse-engineering-fundamentals-explained.mp4` |
| Captions | `video/reverse-engineering-fundamentals-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Offensive Security Methodology in Java - With Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (80)
- [ ] tags are at most 500 characters in total — ok (194)
- [ ] description is at most 5,000 characters — ok (249)
- [ ] chapters start at 00:00 and there are at least three
