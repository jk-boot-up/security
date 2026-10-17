# Dependencies

This project uses one Spring module, plus the small libraries it expects the application to supply.

## What spring-security-crypto is

the part of Spring Security that holds the password encoders: bcrypt, PBKDF2, scrypt and Argon2, the delegating encoder, and some old encoders it marks as deprecated. It has no web or database parts, so it can be used without the rest of Spring Security.

## What Bouncy Castle is

an open-source cryptography library. Spring's Argon2 and scrypt encoders use its implementations, so the library has to be on the class path for those two.

## What spring-core is

Spring's utility classes and its logging. The crypto module uses them but leaves it to the application to include them.

## Why this project uses them

The plain project builds a slow, salted hash by hand so the ideas are visible. Real applications should not write
their own: a library has been reviewed by many people, supports several algorithms, and keeps the stored format and the upgrade path in one place. This
project shows the library doing what the plain project did by hand.

## What to install

Only a JDK. Gradle downloads the rest, and the versions are pinned:

| Tool | Version |
| --- | --- |
| spring-security-crypto | 7.1.1 |
| spring-core | 7.0.9 |
| Bouncy Castle (bcprov-jdk18on) | 1.86 |
| JUnit | 5.10.2 |

## What it costs

- Argon2 and scrypt cannot be used without Bouncy Castle, which is a large library for two encoders.
- The crypto module's own dependencies are left to the application, so a missing logging or core class shows up as an error when the demo first runs.
- Defaults change between versions of the library, so the settings of a stored text must be read from the text, not assumed.
