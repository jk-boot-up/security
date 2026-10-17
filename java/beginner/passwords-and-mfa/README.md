# Passwords and multi-factor login

How passwords are stored, how they are guessed at, and how a second factor makes a stolen password not enough. Hashing is counted in steps and attacks run on a clock made of ticks, so every number repeats.

## The projects

| # | Project | What it shows |
| --- | --- | --- |
| 1 | [password-hashing](password-hashing) | Plain text, a fast hash, a salted hash and PBKDF2, with the attacker's work counted in steps |
| 2 | [password-hashing-with-spring-security-crypto](password-hashing-with-spring-security-crypto) | The same story with Spring Security's encoders: bcrypt, PBKDF2, scrypt, Argon2, and a migration at login |
| 3 | [password-attacks-and-defences](password-attacks-and-defences) | A bank's login against dictionary, spraying and credential-stuffing attacks, with lockout, rate limit, second factor and breach check |
| 4 | [password-attacks-and-defences-with-nginx](password-attacks-and-defences-with-nginx) | A real nginx rate limit in Docker, and the key it counts: address, header or username |
| 5 | [totp-multi-factor](totp-multi-factor) | One-time codes built from HMAC, checked against RFC 4226 and RFC 6238 |
| 6 | [totp-multi-factor-with-java-otp](totp-multi-factor-with-java-otp) | The same codes from the java-otp library, and what the library leaves to the bank |

A project with `-with-` in its name is the same lesson with a framework, a library or a local tool, and is its own self-contained project.

## How the module is judged

- **Break it first.** Each project shows the naive version failing before any control appears.
- **Every claim is a number.** Each failure and each fix prints an exact result that a test checks, against a golden output file.
- **The bill is part of the lesson.** The last act of every project shows what the control costs, or what it still leaves open.
- **A threat model for every project,** with a diagram, in `docs/threat-model.md`.

The shared decisions are in [`docs/spec.md`](docs/spec.md) and [`docs/implementation-plan.md`](docs/implementation-plan.md).
