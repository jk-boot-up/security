# Spec — Path Traversal and File Upload with Spring Boot

## Purpose

Teach path traversal, unsafe upload and open redirect on a real Spring Boot application, including what Spring's own handler and multipart limits do.

## Not in scope

- Antivirus scanning of uploads.
- Image processing libraries and their own parsing risks.
- Object storage and signed addresses, which are described only.

## The problem

Servers use names and paths from requests as if they were their own.

## The pieces

| Role | Class |
| --- | --- |
| Downloads | `NaiveDownloads`, `PrefixDownloads`, `SafeDownloads` |
| Uploads | `NaiveUploads`, `SafeUploads`, `Sniffer`, `WebRoot` |
| Redirects | `ReturnLink`, `Redirects` |
| The web layer | `FilesController`, `StoreConfig`, `Wire` |
| Entry point | `FilesSpringDemo` |

## Requirements

1. A name with dot-dot, and a sibling folder, are read by the naive and the prefix versions.
2. The fixed download refuses a bad name, a path outside, an encoded spelling and a link that leaves the folder.
3. Spring's static handler is shown refusing dot-dot and following a link.
4. The fixed upload makes its own name, decides the type from bytes and forces a download.
5. Spring's multipart limit refuses a large upload before the controller runs.
6. The return address is only ever a path on this site.
7. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 11, all passing |
| Demo output | 47 lines, identical on every run |
| Acts | 6 |
| Video | 06:57, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
