# Problem Statement

## The scenario

A bank wants to know what its TLS endpoints still accept, who could read old traffic, and what a newer protocol version would remove.

## The naive version

Run one scanner, accept the grade, and keep the settings that the scanner does not name.

## What this project must deliver

- Three local servers with planted weaknesses.
- Probes and testssl.sh scored against the truth.
- A downgrade, the fallback signal and a minimum version.
- A recorded session opened with a leaked key, and a TLS 1.3 only server.
