# One-Time Codes (TOTP), Explained

## The idea in one sentence

A time-based one-time password is a code made by putting the number of 30-second steps since 1970 through HMAC with a secret that the phone and the bank share, so both can work it out without talking and a stolen password alone is no longer enough.

## The 6 acts

### 1. A password alone

Dana's password, midnight7, leaked from another site. THE LOGIN: the user dana, that password, and no code, because the bank asks
only for the password. THE ANSWER: OK. One factor means one leak is enough.

### 2. Enrolling: one secret, shared once

Dana enrols. BEFORE ENCODING: the bank makes a 20-byte secret; the demo uses the published RFC test key, 12345678901234567890.
AFTER ENCODING: the secret as Base32 text, GEZDGNBV GY3TQOJQ GEZDGNBV GY3TQOJQ, which the app takes in by QR code or by typing. Base32 cuts the
bytes into groups of 5 bits, and each group is one of 32 letters and digits, so 5 bytes become 8 letters. From then on the phone and the
bank each work out a code from the secret and the time step: 755224, 287082, 359152 and 969429 for steps 0 to 3, each good for 30 seconds.
These are the test values published in RFC 4226.

### 3. How the code is made, step by step

BEFORE: the secret and the time, 59 seconds. Step 1: the counter is 59 / 30 = 1, written as 8 bytes. Step 2: HMAC-SHA1 of the counter, with the
secret as key, gives 20 bytes: 75a48a19 d4cbe100 644e8ac1 397eea74 7a2d33ab. Step 3: the last byte is ab, and its last half-byte, b, is 11, so 4 bytes
are read from byte 11: c1397eea. Step 4: the top bit is dropped, so the number is never negative, 1094287082, and the last six digits are kept.
AFTER: the code 287082, the same as `Totp` gives. `CodeSteps` does these steps one by one, and a test checks it against `Totp` and the RFC 4226 values.

### 4. Logging in, and clocks that disagree

THE ATTEMPTS, at time 59, which is step 1: the password alone; 287082 from a phone on time; a wrong code, 000000; 359152 from a phone 25 seconds
fast, which is already in step 2; and 338314 from a phone 70 seconds fast, in step 4. THE ANSWERS: the bank accepts its own step and one step
either side, a window of one, so steps 0 to 2. The password alone gets CODE_NEEDED, the phone on time and the phone 25 seconds fast get OK, and the
wrong code and the phone 70 seconds fast, three steps ahead, get WRONG_CODE.

### 5. The same code twice

THE LOGINS: Dana logs in with 287082 at time 59, and an observer who saw her type it uses the same code at time 64. WITHOUT REPLAY PROTECTION
both get OK: a replay. WITH REPLAY PROTECTION the bank remembers the last step it accepted for each customer, step 1, and refuses any code from that
step or an earlier one: the observer gets REPLAYED. The code now works once, which is why it is called a one-time password.

### 6. The bill: what a code cannot do

Three costs. A FAKE BANK PAGE: Dana types midnight7 and 287082 into it, and it passes both to the real bank at time 61, which says OK; the code
cannot tell the real bank from a copy. A STOLEN SECRETS TABLE: the bank must keep Dana's secret to check codes, so the attacker's own app shows
287082 at time 59, the same as Dana's. A LOST PHONE: Dana types recovery-1, which is OK once and WRONG_CODE the second time; until it is used it is a
second way in, and must be guarded like the password.

## The verdict

Use an authenticator app, not text messages, as a second factor for anything that matters. Accept a small window, refuse a
code that was already used, keep secrets encrypted on the server, guard recovery codes like passwords, and know that a code
does not stop a fake page. For the highest stakes, offer a hardware security key as well.

## How to recognise this in code you did not write

- A six-digit code in an authenticator app that changes every 30 seconds.
- A QR code shown when two-step sign-in is turned on.
- A page of recovery codes to print and keep.
- `otpauth://` links, which carry the secret, the name and the step.

## Where you have already met this

- The six-digit code in Google Authenticator, Microsoft Authenticator or a similar app, which changes every 30 seconds.
- The QR code you scan when you turn on two-factor sign-in, which carries the secret.
- "Enter one of your recovery codes" when you have lost your phone.
- Banks and email providers saying that SMS codes are weaker than an app.
