# Password Hashing with Spring Security Crypto, Explained

## The idea in one sentence

Spring Security's password encoders give every algorithm the same two calls, add a salt for you, write the algorithm's name in front of the stored text so old texts can still be checked and upgraded, and are only safe when the list of algorithms they accept is chosen on purpose.

## The 6 acts

### 1. Two weak encoders, written out here

The library keeps the two weakest choices only for old systems, and marks them deprecated, so this project writes them out in a few lines.
Before hashing, Ana's password is welcome1. After hashing with the plain-text encoder, the stored value is welcome1: the password itself.
After hashing with a fast SHA-256 encoder, the stored value is a random salt followed by a hash of the salt and the password, so Ana and Ben get
different values. But SHA-256 is one quick step, so the attacker's eight common passwords crack 4 of the 5 accounts in 24 guesses.

### 2. One call: the delegating encoder

`PasswordEncoderFactories.createDelegatingPasswordEncoder()` gives an encoder that is ready to use; the demo builds the same one, bcrypt at cost 10, with repeatable salts so
the values in this lesson never change. Before hashing, Ana's password is welcome1. After hashing, the stored text is 68 characters: `{bcrypt}` names the algorithm, `$2a$` is bcrypt's version,
`10` is the cost (2^10 = 1024 rounds), then 22 characters of salt and 31 of hash. Ben, with the same password, gets a different salt, so a different stored text.
A login is checked in three steps: read the cost and the salt from the stored text, hash the typed password again with them, and compare with the stored hash.
welcome1 gives the same hash, so true; guess gives a different one, so false. The library's own `matches` gives the same answers.

### 3. Four algorithms, one interface

Four algorithms sit behind the same interface. Before hashing, the password is welcome1. After hashing, bcrypt gives 60 characters beginning `$2a$10$`;
PBKDF2 gives 96 characters, the salt and the hash in hex, with its settings kept in the code; scrypt gives 77 beginning `$100801$`, its CPU cost, memory and lanes packed together;
and Argon2 gives 97 beginning `$argon2id$v=19$m=16384,t=2,p=1$`: version 19, 16384 KiB of memory, 2 passes and 1 lane. Each one matches welcome1,
and swapping one for another is a change of one line.

### 4. The same attack, at two costs

The attacker tries the eight common passwords in turn on each account, and stops at the first one that matches: 3 guesses for Ana and Ben, 4 for Carol, 6 for Erin and all 8 for Dave,
24 in all. At bcrypt cost 4 each guess is 16 rounds; at cost 10 it is 1024 rounds. Both lose the same four accounts, but each guess at cost 10 is 64 times the work.
That is what makes guessing slow. Dave survives only because his password is not on the list.

### 5. Moving to a stronger algorithm without asking anyone

Before the login, Erin's stored text begins `{bcrypt}$2a$10$`, and the shop has switched its default to Argon2. Erin logs in with letmein: the id `{bcrypt}` picks bcrypt,
so matches gives true. `upgradeEncoding` answers true, because `{bcrypt}` is not the default. After the login the shop stores her password again: the new text
begins `{argon2@SpringSecurity_v5_8}` and `$argon2id$v=19$m=16384,t=2,p=1$`, and `upgradeEncoding` now says false. Nobody had to reset a password.

### 6. The bill: an open list, and memory

Two costs. First, the default delegating encoder knows many old forms, including `{noop}`, which means no hashing. Someone who can write to the accounts table plants `{noop}letmein`
in Erin's row, and the default encoder says letmein matches: true. A delegating encoder built with an explicit list, bcrypt and Argon2 only, refuses it: no encoder is mapped for that id.
Second, memory. One Argon2 hash uses 16384 KiB, which is 16 MiB, and 100 logins at once need 1,600 MiB. That memory is what makes it hard to speed up, and the shop pays for it.

## The verdict

Use a vetted library, never your own hashing. Take the encoder for a modern algorithm, bcrypt or Argon2, with
a cost you have measured, build the delegating encoder from an explicit list, and upgrade stored texts at login with
`upgradeEncoding`. Remember the limits: a strong algorithm does not save a weak password, and every login pays
its price in time, and for Argon2 in memory.

## How to recognise this in code you did not write

- A `PasswordEncoder` bean in a configuration class.
- Stored passwords that start with `{bcrypt}` or `$argon2id$`.
- A call to `upgradeEncoding` after a successful login.
- A `DelegatingPasswordEncoder` built with a map of encoders.

## Where you have already met this

- `PasswordEncoder` beans in Spring applications, and `{bcrypt}` in their user tables.
- The `$2a$10$` start of bcrypt hashes in many databases.
- `$argon2id$v=19$m=16384,t=2,p=1$...`, the format that shows Argon2's settings.
- Migration notes that say "hashes are upgraded the next time a user logs in".
