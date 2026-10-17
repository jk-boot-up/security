# Password Hashing with Spring Security Crypto, Explained

## The idea in one sentence

Spring Security's password encoders give every algorithm the same two calls, add a salt for you, write the algorithm's name in front of the stored text so old texts can still be checked and upgraded, and are only safe when the list of algorithms they accept is chosen on purpose.

## The 6 acts

### 1. Two the library marks as deprecated

The library still contains the two weakest choices, and marks them deprecated. NoOpPasswordEncoder stores
the password as it is: ana's row says welcome1. MessageDigestPasswordEncoder is a plain SHA-256 hash with
a salt, so Ana's and Ben's stored values differ. But it is one cheap step per guess, so the same eight
common passwords crack 4 of the 5 accounts in 24 guesses. Both exist for old systems, not for new ones.

### 2. One call: the delegating encoder

`PasswordEncoderFactories.createDelegatingPasswordEncoder()` gives an encoder that is ready to use. Ana's stored
text begins `{bcrypt}` and is 68 characters long. Encoding welcome1 twice gives two different texts, because a new
salt is added each time, with no code from the shop. `matches` checks a login: the right password gives true,
a wrong one false. The name in front is how the encoder knows which algorithm to use when it checks the text again.

### 3. Four algorithms, one interface

Four algorithms sit behind the same interface. BCrypt's stored text begins $2a$10$: cost 10, which is
1024 rounds. PBKDF2 gives a text of 96 characters, and scrypt one of 77. Argon2 writes its settings
in the text: m=16384, t=2 and p=1, which are 16384 KiB of memory, 2 passes and 1 lane of parallelism.
Each one checks its own hash and rejects a wrong password, and swapping one for another is a change
of one line.

### 4. The same attack, at two costs

The attacker runs the same eight-word list against bcrypt at the lowest cost, 4, and at the library's default,
10. Both lose the same four accounts after the same 24 guesses. The difference is the work behind each guess:
bcrypt's work doubles with every step of cost, so cost 10 is 64 times the work of cost 4. That is what
makes guessing slow. And as in the plain project, Dave survives only because his password is not on the list.

### 5. Moving to a stronger algorithm without asking anyone

Erin's stored text begins `{bcrypt}`. The shop switches its default algorithm to Argon2. Erin logs in, and the
old text still checks: matches gives true, because the name in front selects bcrypt for it. The encoder's
`upgradeEncoding` answers true, because the text is not in the current default. The shop stores Erin's password again;
the new text begins `{argon2@SpringSecurity_v5_8}`, and the next check of `upgradeEncoding` says false. Nobody
had to reset a password, and only active customers have been upgraded.

### 6. The bill: an open list, and memory

Two costs. First, the default delegating encoder knows many old forms, including `{noop}`, which means no hashing.
It says `{noop}letmein` matches the password letmein. Anyone who can write to the accounts table could plant such a value for
an account they control. A delegating encoder built with an explicit list, bcrypt and Argon2 only, refuses
it: no encoder is mapped for that name. Second, memory. One Argon2 hash uses 16384 KiB, which is 16 MiB, and
100 logins at once need 1,600 MiB. That memory is what makes it hard to speed up, and the shop pays for it.

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
