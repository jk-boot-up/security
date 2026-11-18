# YouTube sheet — Insecure Deserialization and Its Defences

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Insecure Deserialization and Its Defences in Java

## Description

Insecure deserialization and its defences, in Java. Keeping a shopping cart in a cookie as a serialized Java object looks easy, but the bytes decide which classes the server builds, and some classes run code while they are being read: a customer who edits the cookie can lower a price, or make the server delete a file before it even sees that the cookie is not a cart. Explained with an online store, like a flat-pack kit whose instructions you must follow while you unpack it, written by whoever packed the box. We add an allow-list ObjectInputFilter that refuses unknown classes before they are built, see that it still accepts a forged price, then replace the object with plain, signed data: product codes and quantities, an HMAC checked first, and prices looked up on the server. We finish with the cost: an allow-list needs upkeep, and signing needs a key.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/intermediate/web-security/insecure-deserialization

Chapters:

```
00:00 Introduction
00:56 The Scenario
01:32 The Words
02:08 What the Letters Stand For
03:02 What the Letters Stand For (continued)
03:13 The bytes choose the classes
04:09 A cart becomes a cookie, and comes back
05:18 Act One — A cart in a cookie
05:31 Act One — A cart in a cookie
05:53 A lie, and a class that runs code while it is read
06:57 Act Two — The cookie decides what the server builds
07:14 Act Two — The cookie decides what the server builds
07:39 The filter answers before each class is built
08:45 Act Three — An allow-list filter names the classes of a cart
09:02 Act Three — An allow-list filter names the classes of a cart
09:24 Plain text, and a signature only the server can make
10:34 Act Four — Data only: product codes and quantities, signed
10:58 Act Four — Data only: product codes and quantities, signed
11:19 What It Costs
12:00 Where You Have Seen It
12:31 How To Use It
13:04 The Java Classes
13:17 SerializedCartCookie — readObject, and the type check after it
13:35 SerializedCartCookie — the allow-list
13:48 TempCleaner — its own readObject
14:04 SignedTextCartCookie — the signature first, then the data
14:22 Thanks for Watching
```

## Tags

insecure deserialization, objectinputfilter, gadget chain, owasp top 10, java, application security, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/insecure-deserialization-explained.mp4` |
| Captions | `video/insecure-deserialization-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Web Security in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (49)
- [ ] tags are at most 500 characters in total — ok (145)
- [ ] description is at most 5,000 characters — ok (862)
- [ ] chapters start at 00:00 and there are at least three
