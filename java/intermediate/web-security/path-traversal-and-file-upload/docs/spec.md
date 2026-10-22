# Spec — Path Traversal, File Upload and Open Redirects

## Purpose

Teach path traversal, unrestricted upload and open redirects by attacking a store's invoice download, photo upload and return link in a real temporary folder, then fixing each in layers.

## Not in scope

- Malware scanning and image processing libraries.
- Cloud object storage.
- The Spring multipart version: the path-traversal-and-file-upload-with-spring-boot project.

## The problem

Developers join customer text to paths and addresses, and save what customers send, so the customer decides where files go and where people are sent.

## The pieces

| Role | Class |
| --- | --- |
| The file tree | `Layout` |
| Downloads | `Downloads`, `NaiveDownloads`, `PrefixDownloads`, `SafeDownloads` |
| Uploads and serving | `Uploads`, `NaiveUploads`, `SafeUploads`, `Sniffer`, `WebRoot`, `Served` |
| Redirects | `ReturnLink` |
| Entry point | `FilesDemo` |

## Requirements

1. The naive download reads files outside the invoices folder.
2. A text-prefix check still reads a sibling folder with a similar name.
3. The safe download refuses `..`, encoded dots, other folders and links that lead outside, and still reads honest invoices.
4. The naive upload replaces a settings file and serves a script inline; the safe one ignores the customer's name, decides by the bytes, limits size and serves as a download.
5. The safe return link only allows paths on this site, and the host check is exact.
6. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 11, all passing |
| Demo output | 42 lines, identical on every run |
| Acts | 6 |
| Video | 07:22, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
