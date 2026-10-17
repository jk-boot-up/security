# One-Time Codes with the java-otp Library, Explained

## The idea in one sentence

A library such as java-otp makes the time-based one-time code and checks it for a single step, and everything around that, the window for drifting clocks, the memory that stops a code being used twice, the storage of secrets and the limits on guessing, is still the bank's own code.

## The 5 acts

### 1. The library makes the same codes as the hand-written version

The library's generator, given the RFC's key and the times 0, 30, 60 and 90, gives 755224, 287082, 359152 and 969429. These are the same codes as the
hand-written version printed, and the values published in RFC 4226. At time 59 the code is 287082. A library that did not match the standard would be useless
with real authenticator apps, so checking it against the published values is the first thing to do.

### 2. What the library checks, and what it leaves to the bank

The library can validate a code, but only for the single 30-second step that the given time is in. Code 287082 is right at time 59, in step 1, and at
time 40, also step 1. At time 65, which is step 2, the library says false. That is correct for a library, and too strict for a bank, because phone clocks drift. So the
bank wraps it in a window of one step either side: at time 65 the bank accepts the code, and at time 95, in step 3, two steps on, it is refused.

### 3. Other hashes and longer codes

The library can use HMAC with SHA-1, SHA-256 or SHA-512, and codes of other lengths. At time 59, with 8 digits, SHA-1 gives 94287082, SHA-256 gives
46119246 and SHA-512 gives 90693936, which are the values in RFC 6238. This is what the standard allows. What a customer's phone supports is another matter, because some
authenticator apps ignore these settings and use SHA-1, 6 digits and 30 seconds. A bank that changes them must control the app, or test the ones its customers use.

### 4. Giving the secret to the phone

How does the secret get onto the phone? The bank shows a QR code. It is a picture of text: an `otpauth://` link holding the issuer, the account, the secret in
Base32, the hash, the number of digits and the period. For the demo's key it reads otpauth://totp/Example%20Bank:dana%40example.com?secret=GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ&issuer=Example%20Bank&algorithm=SHA1&digits=6&period=30.
The app reads it once and keeps the secret, which is why the screen showing it must be private, and why it should be shown once.

### 5. The bill: what the library does not do

With the library alone, the same code works twice. A first login with 287082 at time 59 is OK, and a second at time 64 is also OK. That is not the library's job. The
bank's own memory of the last accepted step makes the second use REPLAYED. And the library keeps no secrets, counts no wrong codes and limits no attempts. The window, the
replay memory, secure storage of the secrets, and the limits on guessing are the bank's to write, and to test.

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
