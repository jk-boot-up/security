# Problem Statement

## The scenario

An online bank's customers have passwords, and some of those leak from other sites. The bank wants a second factor that
a stolen password cannot supply, that works offline on a phone, and that needs no new hardware.

## The naive version

Rely on the password alone, or send a code by text message. The first fails to one leak, and text messages can be
intercepted or redirected by a SIM swap.

## What this project must deliver

- HOTP and TOTP written from the standard library, checked against the published values of RFC 4226 and RFC 6238.
- Base32 for the secret, checked against RFC 4648.
- A bank that asks for a code after the password, with a window for clocks that are a little out.
- Replay protection that makes a code work once.
- Recovery codes that work once.
- A demonstration of what TOTP cannot stop.
- The same output every run, checked against a golden output file.
