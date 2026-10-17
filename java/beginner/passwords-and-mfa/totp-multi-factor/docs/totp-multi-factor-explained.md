# One-Time Codes (TOTP), Explained

## The idea in one sentence

A time-based one-time password is a code made by putting the number of 30-second steps since 1970 through HMAC with a secret that the phone and the bank share, so both can work it out without talking and a stolen password alone is no longer enough.

## The 5 acts

### 1. A password alone

Dana's password, midnight7, leaked from another site. The bank checks only the password. The attacker logs in and the
answer is OK. One factor means one leak is enough.

### 2. Enrolling a second factor

Dana enrols. The bank makes a 20-byte secret and shows it as Base32 text, which the phone takes in by QR code or by typing:
GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ. The phone and the bank each turn the secret and the time into a code. At time 59 the counter, the
number of whole 30-second steps, is 1, and the code is 287082. The codes for steps 0, 1, 2 and 3 are 755224, 287082, 359152 and 969429, each good for
30 seconds. They match the test values published in RFC 4226. The phone and the bank never talk: they only share the secret and the clock.

### 3. Logging in with a code, and clocks that disagree

With the second factor on, the password alone gets CODE_NEEDED. Dana's phone shows 287082 at time 59, and the login is OK. A wrong
code, 000000, is WRONG_CODE. Phones' clocks are never exactly right, so the bank accepts the code for the current step and for one
step either side, a window of one. A phone 25 seconds fast shows the next step's code, 359152, and is accepted. A phone 70 seconds fast shows the code
of a step three ahead, 338314, and is not. An old code from step 2, used at time 120, which is step 4, is also refused.

### 4. The same code twice

A code is good for 30 seconds, plus the window. If someone sees Dana type it, they can use it too, within that time. Without any
memory, the bank accepts the same code twice: dana logs in with 287082, and an observer uses it 5 seconds later,
also OK: a replay. With replay protection, the bank remembers the last step it accepted for each customer, and refuses any code from that step or an
earlier one. The observer gets REPLAYED. The code now works once, which is why it is called a one-time password.

### 5. The bill: what a code cannot do

Three costs. First, a fake bank page asks Dana for her password and her code. The attacker passes both to the real bank at once,
within the code's 30 seconds, and the login is OK. The code cannot tell the real bank from a copy. Second, the bank must keep the secret in
order to check codes. If its table is stolen, the attacker's own app shows 287082 at time 59, the same as Dana's. The secret lives on the
server too. Third, a customer who loses the phone needs a recovery code. It works once, and the second use is refused, but until it is used it is
a second way in, and must be guarded like a password.

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
