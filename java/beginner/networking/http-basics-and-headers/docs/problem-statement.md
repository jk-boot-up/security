# Problem Statement

## The scenario

An online store sits behind a proxy and receives requests from browsers and from anyone else. It sets a login cookie and sends pages that browsers must handle safely.

## The naive version

Trust whatever arrives, copy customer text into headers, set cookies with defaults, and leave the browser to guess.

## What this project must deliver

- A strict HTTP/1.1 request parser and a response writer that refuse ambiguity and line breaks in headers.
- A cookie judge for HttpOnly, Secure, SameSite and the __Host- prefix.
- A header auditor for five security headers that checks values and not only presence.
- A real server and client on this machine, so the audit runs on headers that crossed a socket.
- The same facts printed on every run.
