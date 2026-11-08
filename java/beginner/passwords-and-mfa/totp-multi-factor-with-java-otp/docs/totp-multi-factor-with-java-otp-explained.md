# One-Time Codes with the java-otp Library, Explained

## The idea in one sentence

A library such as java-otp makes the time-based one-time code and checks it for a single step, and everything around that, the window for drifting clocks, the memory that stops a code being used twice, the storage of secrets and the limits on guessing, is still the bank's own code.

## The 6 acts

### 1. The library's codes, checked against the standard

Before: the inputs are the RFC's test key, 12345678901234567890, and a time in seconds since 1970. Time is cut into steps of 30 seconds, and each step has its
own code. After: the library gives 755224 for step 0, 287082 for step 1, 359152 for step 2 and 969429 for step 3. Time 59 is still in step 1, so it gives
287082 again, the value published in RFC 6238. A library that did not match the standard would be useless with real authenticator apps, so this is the first check.

### 2. How the code at time 59 is made, step by step

The library hides how a code is made, so `TotpSteps` redoes it by hand. Step 1: 59 / 30 = 1, written as 8 bytes, 00000000 00000001. Step 2: HMAC-SHA1 of those
bytes with the secret key gives 20 bytes, 75a48a19 d4cbe100 644e8ac1 397eea74 7a2d33ab. Step 3: the last byte is ab; its last half-byte, b, is 11, so take the 4 bytes
starting at byte 11: c1397eea. Step 4: drop the top bit (so the number is never negative) and read it as a number, 1094287082, then keep the last 6 digits: 287082.
That is exactly the library's code, and a test checks it for many times. Without the key nobody can make the HMAC, so nobody can make the code.

### 3. What the library checks, and what the bank adds

The code 287082 was made at time 59, in step 1. Before: the library alone validates a code for one 30-second step only. It says true at times 59 and 40, both in step 1,
and false at time 65, in step 2. That is correct for a library, and too strict for a bank, because phone clocks drift and typing takes time. After: the bank wraps it in
a window of one step either side. At time 65 the bank says OK, and at time 95, in step 3, two steps on, it says WRONG_CODE. The window is the bank's code.

### 4. Other hashes and longer codes

Before: the settings, at time 59. SHA-1 with a 20-byte key, SHA-256 with a 32-byte key and SHA-512 with a 64-byte key, all with 8 digits. After: 94287082, 46119246
and 90693936, the values in RFC 6238. The SHA-1 code is the number of act two with two more digits kept. This is what the standard allows; many authenticator apps
take only SHA-1, 6 digits and 30 seconds, so a bank that changes them must control the app, or test the ones its customers use.

### 5. Giving the secret to the phone

Before: what the bank puts in: the issuer Example Bank, the account dana@example.com, the 20-byte secret and the settings SHA1, 6 digits, 30 seconds. After: one
`otpauth://` link, which the QR code carries: otpauth://totp/Example%20Bank:dana%40example.com?secret=GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ&issuer=Example%20Bank&algorithm=SHA1&digits=6&period=30.
The secret is written in Base32: each letter carries 5 bits, so 20 bytes (160 bits) become 32 letters. The app reads the link once and keeps the secret, which is
why the screen showing it must be private, and why it should be shown only once.

### 6. The bill: what the library does not do

Before the fix: with the library alone, a login with 287082 at time 59 is OK, and the same code at time 64 is OK again: a code used twice. After the fix: the bank
remembers, for each customer, the last step it accepted, so the second use is REPLAYED. And the library keeps no secrets, counts no wrong codes and limits no attempts.
Those, and the choice of window, are the bank's to write, and to test.

## The verdict

Use a library for the code, never your own HMAC. Check it against the standard's values once, wrap its single-step check in a window and a replay memory, keep secrets encrypted, limit attempts, and test every
one of those. The library does the arithmetic. The bank is responsible for the policy.

## How to recognise this in code you did not write

- `TimeBasedOneTimePasswordGenerator` or its equivalent in other languages.
- `otpauth://totp/` links in sign-up pages for two-step sign-in.
- Documentation that tells you to prevent replays and store secrets yourself.
- An authentication framework with a "TOTP" option built on a library like this.

## Where you have already met this

- `TimeBasedOneTimePasswordGenerator` and its equivalents in most languages: `pyotp` in Python, `otplib` in JavaScript, `rotp` in Ruby.
- The `otpauth://totp/...` text behind the QR code on a two-step sign-in setup page.
- Library documentation that says "you are responsible for storing the secret and preventing replays".
- Frameworks, such as Spring Security or Keycloak, that wrap a library like this and add the policy around it.
