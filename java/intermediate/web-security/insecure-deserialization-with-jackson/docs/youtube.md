# YouTube sheet — Insecure Deserialization with Jackson

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Insecure Deserialization with Jackson 3: Polymorphic Typing Traps and Their Fixes

## Description

Insecure deserialization with Jackson 3, in Java. JSON is only data until a type id tells Jackson which class to build: when a field is typed by a class name taken from the data, whoever writes the cookie chooses the class, and a class from another team's plug-in can delete a file just by being built. Explained with an online store's cart cookie, like a flat-pack kit that names its own parts, and a factory that will fetch any part named on the box. We see that Jackson 3 refuses class names for an Object field by default, but not for an ordinary interface or with global default typing, then fix it three ways: short names from a list, a validator that lists the allowed classes, or no types in the data at all. We finish with data-only requests priced by the shop, strict binding that refuses a forged price, and limits on nesting depth and length.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/intermediate/web-security/insecure-deserialization-with-jackson

Chapters:

```
00:00 Introduction
00:52 The Scenario
01:20 Act One — A cart in a cookie
01:41 Act Two — The cookie decides which class is built
02:14 Act Three — A list of kinds, or a list of classes
02:39 Act Four — Data only, priced by the shop
03:05 Act Five — Limits on the stream
03:26 What It Costs
04:02 Who Does What
04:36 Where You Have Seen It
05:06 How To Use It
05:42 Thanks for Watching
```

## Tags

jackson, deserialization, polymorphic typing, gadget, java, application security, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/insecure-deserialization-with-jackson-explained.mp4` |
| Captions | `video/insecure-deserialization-with-jackson-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Web Security in Java - With Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (81)
- [ ] tags are at most 500 characters in total — ok (126)
- [ ] description is at most 5,000 characters — ok (854)
- [ ] chapters start at 00:00 and there are at least three
