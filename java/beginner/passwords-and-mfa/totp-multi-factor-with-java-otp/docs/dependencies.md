# Dependencies

This project uses two small libraries, both from Maven Central, with no other dependencies.

## What java-otp is

an open-source Java library for HOTP and TOTP, written by Jon Chambers. `TimeBasedOneTimePasswordGenerator` makes a code for a secret key and a time, and can validate a code for one time step. It does nothing else.

## What Apache Commons Codec is

a library of encoders and decoders. Here it supplies Base32, the text form of the secret in the `otpauth://` link.

## Why this project uses them

The plain project writes HOTP and TOTP by hand so the recipe is clear. Production code should use a library that many people have reviewed. This project shows what one gives, and, more importantly, the line where it stops and the bank's own code begins.

## What to install

Only a JDK. Gradle downloads the two libraries, and the versions are pinned:

| Tool | Version |
| --- | --- |
| java-otp | 1.0.0 |
| Apache Commons Codec | 1.23.0 |
| Java | 25 |
| JUnit | 6.1.3 |

## What it costs

- Two more dependencies to keep up to date, for what is a few dozen lines of standard-library code.
- The library's choices, such as how it formats a code, are its own, and the tests against the RFCs are what keep them honest.
- Replacing the library later means the checks around it must be re-tested.
