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
01:21 Act One — Downloading an invoice
01:36 Act Two — A name with dot-dot leaves the folder
01:55 Act Three — A text check on the path is not a path check
02:14 Act Four — Names, paths and links, all checked
02:40 Act Five — Uploads
03:06 Act Six — Where to send the customer next
03:26 What It Costs
04:04 Who Does What
04:44 Where You Have Seen It
05:18 How To Use It
05:52 Thanks for Watching
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
