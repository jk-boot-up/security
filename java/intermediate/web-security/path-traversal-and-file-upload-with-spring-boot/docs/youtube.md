# YouTube sheet — Path Traversal and File Upload with Spring Boot

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Path Traversal and File Upload in Spring Boot: Multipart, Resource Handler and Open Redirects

## Description

Path traversal and file upload in Spring Boot 4, in Java. Spring hands a controller the customer's file name exactly as it was sent, in a query parameter or from MultipartFile.getOriginalFilename, so a name with two dots walks out of the invoice folder, a text check lets a folder with a similar name through, and an upload keeps a name that overwrites the settings. Explained with an online store, like a cloakroom ticket that someone has rewritten to point at the manager's office. We see that Spring's own static handler refuses two dots but follows a symbolic link out of its folder, then fix the shop with a name pattern, a real-path check, server-made names, a type decided from the bytes, downloads with nosniff and multipart size limits, and a return address that is only ever a path on the site.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/intermediate/web-security/path-traversal-and-file-upload-with-spring-boot

Chapters:

```
00:00 Introduction
00:50 The Scenario
01:21 The Words
02:06 What the Letters Stand For
03:10 What the Letters Stand For (continued)
04:09 A name that walks out of its folder
05:09 The folders, one request, one path
06:08 Act One — An honest name downloads one invoice
06:20 Act One — An honest name downloads one invoice
06:35 Two dots cancel the folder before them
07:39 Act Two — A name with .. climbs out of the folder
07:57 Act Two — A name with .. climbs out of the folder
08:06 Same letters, different folder
09:07 Act Three — A text check is not a path check
09:21 Act Three — A text check is not a path check
09:36 Six requests, four layers
10:59 Act Four — The fixed version checks in four layers
11:09 Act Four — The fixed version checks in four layers
11:31 Act Four — The fixed version checks in four layers
11:50 Dot-dot is refused; the link is served
12:51 Act Five — Spring's static handler follows a link
13:05 Three uploads, saved as they came
14:11 Act Six — The naive upload trusts the customer's name
14:24 Act Six — The naive upload trusts the customer's name
14:38 The bytes and the size decide; the name does not
15:49 Act Seven — The fixed upload decides for itself
16:06 Act Seven — The fixed upload decides for itself
16:21 Act Seven — The fixed upload decides for itself
16:31 Where each link sends the customer
17:39 Act Eight — Return links stay on our own site
17:52 Act Eight — Return links stay on our own site
18:06 What It Costs
18:44 Where You Have Seen It
19:18 How To Use It
19:53 The Java Classes
20:06 FilesController — a parameter, and getOriginalFilename()
20:22 PrefixDownloads — a text prefix
20:34 SafeDownloads — no second decode; shape, path, real path
20:59 StoreConfig — a static resource handler
21:12 Sniffer — magic bytes
21:32 SafeUploads — the server's own name
21:49 ReturnLink — only a path on our site
22:06 Thanks for Watching
```

## Tags

path traversal, file upload, spring boot, multipart, open redirect, java, application security, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/path-traversal-and-file-upload-with-spring-boot-explained.mp4` |
| Captions | `video/path-traversal-and-file-upload-with-spring-boot-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Web Security in Java - With Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (93)
- [ ] tags are at most 500 characters in total — ok (139)
- [ ] description is at most 5,000 characters — ok (804)
- [ ] chapters start at 00:00 and there are at least three
