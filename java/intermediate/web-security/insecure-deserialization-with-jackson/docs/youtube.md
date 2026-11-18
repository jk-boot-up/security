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
01:20 The Words
01:57 What the Letters Stand For
02:47 The JSON names the class
03:35 A cart becomes JSON, and the note names its class
04:32 Act One — A cart in a cookie
04:46 Act One — A cart in a cookie
05:02 Which note types let the cookie choose the class
06:04 Act Two — The cookie decides which class is built
06:21 Act Two — The cookie decides which class is built
06:39 Act Two — The cookie decides which class is built
06:53 Two lists: names for kinds, or the allowed classes
07:50 Act Three — A list of kinds, or a list of classes
08:11 Act Three — A list of kinds, or a list of classes
08:31 Codes and quantities in, the shop's prices out
09:24 Act Four — Data only, priced by the shop
09:41 Act Four — Data only, priced by the shop
09:57 How deep, how long: the limits on the stream
10:52 Act Five — Limits on the stream
11:13 What It Costs
11:49 Where You Have Seen It
12:18 How To Use It
12:54 The Java Classes
13:07 Cart — @JsonTypeInfo(use = CLASS)
13:22 TempCleaner — a setter with a side effect
13:36 TypedCart — @JsonSubTypes
13:49 Mappers — any class, or only our notes
14:02 Mappers — strict binding and tight limits
14:14 OrderRequest — no price, no type
14:29 Thanks for Watching
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
