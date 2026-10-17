# Dependencies

This project uses Google's Tink library, which brings one dependency of its own, and one JVM option on a recent JDK.

## What Tink is

an open-source cryptography library from Google, designed so that the easy way to use it is also the safe way. It offers small primitives, such as `Aead` for encryption, with sensible, safe defaults and no way to pick unsafe ones.

## What Protocol Buffers is

Google's format for structured data. Tink stores keys and keysets in it, so its Java library comes along with Tink. It uses some unsafe memory access that JDK 24 and later warn about, so the project passes `--sun-misc-unsafe-memory-access=allow` to the run and test tasks.

## Why this project uses them

The plain project shows the mechanism and everything the caller must get right. A library such as Tink shows how that is normally avoided in production: an API with no place to put a nonce or choose a mode, keys in a keyset that can be rotated, and a format that records which key made each message. Putting the two side by side shows what a library takes over, and what it leaves.

## What to install

Only a JDK. Gradle downloads Tink and its dependency, and the versions are pinned:

| Tool | Version |
| --- | --- |
| Tink (com.google.crypto.tink:tink) | 1.24.0 |
| Protocol Buffers (protobuf-java, from Tink) | 4.33.6 |
| Java | 25 |
| JUnit | 5.10.2 |

## What it costs

- A larger dependency than the JDK alone, and a JVM option to silence a warning on JDK 24 and later.
- Messages and keys are in Tink's format, so moving away from Tink means converting them.
- Some functions the demo uses are deprecated in this version, so upgrades need attention.
