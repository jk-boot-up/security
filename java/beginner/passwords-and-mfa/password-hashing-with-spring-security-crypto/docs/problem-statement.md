# Problem Statement

## The scenario

The online store from the plain password-hashing project. It must store five customers' passwords safely, check logins,
and be able to move to a stronger algorithm in future without asking anyone to reset a password.

## The naive version

Use the library's oldest encoders because they are the shortest to write, or write the hashing by hand. The first
leaves the passwords readable or cheap to guess, and the second repeats work the library does better.

## What this project must deliver

- A demonstration of two weak encoders, written out in the project, and what is wrong with each.
- The delegating encoder, with the algorithm's name in front of the stored text and a salt added for you.
- Four algorithms behind one interface, with their visible settings.
- The same attack as the plain project, at two bcrypt costs, with the work compared.
- A migration from bcrypt to Argon2 that happens at login, using `upgradeEncoding`.
- The risk of an open algorithm list, shown and closed with an allow-list.
- The same output every run, checked against a golden output file.
