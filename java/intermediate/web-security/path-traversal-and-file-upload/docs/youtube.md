# YouTube sheet — Path Traversal, File Upload and Open Redirects

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Path Traversal, File Upload and Open Redirects in Java

## Description

Path traversal, unsafe file upload and open redirects, in Java. Three everyday features take a name or a link from a customer: download an invoice, upload a product photo, and return to a page after signing in. A file name with two dots walks out of its folder and reads the database settings, an uploaded file keeps the name and contents the customer chose, and a return address can send a signed-in customer to a look-alike site. Explained with an online store, like a cloakroom ticket that someone has rewritten to point at the manager's office. We watch a text check on the path fail against a folder with a similar name and a symbolic link, then compare paths as paths and follow links first, let the server decide each upload's name and type from its bytes and keep it outside the web root, and send customers only to paths on our own site.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/intermediate/web-security/path-traversal-and-file-upload

Chapters:

```
00:00 Introduction
00:52 The Scenario
01:20 Act One — Downloading an invoice
01:32 Act Two — A name with .. leaves the folder
01:52 Act Three — A text check on the path is not a path check
02:16 Act Four — Names, paths and links, all checked
02:45 Act Five — Uploads
03:16 Act Six — Where to send the customer next
03:49 What It Costs
04:29 Who Does What
05:07 Where You Have Seen It
05:37 How To Use It
06:15 Thanks for Watching
```

## Tags

path traversal, file upload, open redirect, owasp top 10, java, application security, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/path-traversal-and-file-upload-explained.mp4` |
| Captions | `video/path-traversal-and-file-upload-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Web Security in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (54)
- [ ] tags are at most 500 characters in total — ok (130)
- [ ] description is at most 5,000 characters — ok (846)
- [ ] chapters start at 00:00 and there are at least three
