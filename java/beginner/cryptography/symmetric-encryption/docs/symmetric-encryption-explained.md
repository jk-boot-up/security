# Symmetric Encryption (AES-GCM), Explained

## The idea in one sentence

Symmetric encryption uses one secret key to lock and unlock data; AES in ECB mode leaks the pattern of repeated blocks, while AES-GCM hides the data and adds a tag that notices any change, as long as every message gets a nonce that is never reused with the same key.

## The 8 acts

### 1. A statement stored as it is

The bank stores each statement as plain text. A stolen disk gives the thief all eight lines of the statement, and the first line reads SALARY +4000.
There is nothing to decrypt. This is the starting point that encryption is meant to change.

### 2. AES in ECB mode: every block on its own

Each line of the statement is exactly one 16-byte block, and in ECB mode every block is encrypted on its own with the same key. RENT -1200 always becomes
59a345b7 21951fa8 3cf52b5e 42d0fbc9, SALARY +4000 always becomes d0259865 ffa1bf51 fad698aa a1c793eb, and FOOD -0310 becomes f0742ed6 e64e2ee3 e80fd911 38627b51.
The attacker cannot read any line, but sees three different blocks out of eight, and that five of them are the same payment, and where. The pattern of the data shows through.

### 3. AES-GCM, step by step: sealing one line

GCM takes four inputs: the key 64656d6f 2d6b6579 2d313662 79746573, a nonce 00000000 00000000 00000001, the account the line belongs to, account=ANA-1, which is
checked but not hidden (the associated data), and the plain text RENT -1200, whose bytes are 52454e54 20202020 20202d31 3230300a. It gives two outputs: the cipher text
081a4d6d 5de30f22 23d0ee44 b1b85aea and the tag b78e5ab9 bfe2dae4 139b7b0f 168d3c72. The bank stores the nonce, the cipher text and the tag: 12 + 16 + 16 = 44 bytes. The key is kept elsewhere.

### 4. How the tag is made, step by step

The tag of act three is made in four steps. First, a hash key H is made by encrypting sixteen zero bytes with the key: 28a4c82c 939593a9 3e625dd0 c42041f3.
Second, a running value X starts at zero, and each 16-byte block is stirred into it in turn: the account (padded with zeros), the cipher text, and a block holding both
lengths in bits (104 and 128). Each stir is X = (X xor block) times H, where "times" is GCM's own multiplication of 128-bit values, in which every bit of the result
depends on every bit of X and H. X ends as 0546a381 4647a1ad b0a2297a 584e3313. Third, a mask is made by encrypting the nonce followed by the counter 00000001 (the cipher
text itself was made from counter 2 onward): b2c8f938 f9a57b49 a3395275 4ec30f61. Fourth, the tag is X xor the mask: b78e5ab9 bfe2dae4 139b7b0f 168d3c72, exactly
the tag GCM gave. Change one bit of the account or the cipher text and X changes, so the tag changes; without the key, nobody can make H or the mask, so nobody can make a matching tag.

### 5. The same line, sealed again

The bank seals the very same line again. This time the nonce is 2, not 1. The first cipher text was 081a4d6d 5de30f22 23d0ee44 b1b85aea; the second is
e2d8337c daa2332c 8a6558a7 c2860bf9. They have nothing in common, so unlike ECB, nobody looking at the stored data can tell that the two are the same payment.

### 6. Opening it, and three changes

Opening checks the tag before it gives anything back. With the right key and the right account, the line RENT -1200 comes back. Change the first byte of the
cipher text from 08 to 09, one bit, and it is refused. Open it as account=MAL-2, as if it had been moved to another account, and it is refused. Open it with another key,
and it is refused. GCM both hides the data and notices any change, which is what authenticated encryption means.

### 7. The one mistake: using a nonce twice

The one rule of GCM is that a nonce must never be used twice with the same key. Here it is broken: balance=000100, which the attacker knows, and balance=900100, the secret,
are sealed with the same key and the nonce 00000000 00000000 00000000. The two cipher texts differ in one byte only, 45 against 4c, at the place where the plain texts differ, 0 against 9.
GCM scrambles by XORing the message with a keystream made from the key and the nonce, so with one nonce both get the same keystream, and cipher 1 XOR cipher 2 equals plain 1 XOR plain 2:
00000000 00000000 09000000 0000. XOR that with the known message and out comes balance=900100. No key was needed. A repeated nonce can also reveal the key used for the tags, so forgeries become possible too.

### 8. The bill: sizes, nonces, and the key

Encryption has costs. Five bytes seal to 33 bytes and 500 bytes to 528: always 28 more, 12 for the nonce and 16 for the tag, and the length still shows, so a short note and a long one can be told apart. With random
12-byte nonces, NIST allows at most 4,294,967,296 messages under one key, which keeps the chance of a repeated nonce negligible, so the key must be replaced before that. And the key must be stored somewhere safe: a key kept beside the data protects
nothing, and a lost key takes every message with it.

## The verdict

Use AES-GCM, or another authenticated mode, and never ECB. Give every message a fresh nonce, random or carefully counted, and
replace the key well before the limit. Bind messages to their context with associated data. Keep the key in a key store, apart from the data,
and plan for losing and rotating it.

## How to recognise this in code you did not write

- `Cipher.getInstance("AES/GCM/NoPadding")` and a `GCMParameterSpec`.
- A stored value made of a nonce, a cipher text and a tag.
- `AES/ECB` in a code review, which is a finding.
- An advisory titled "nonce reuse" for a library or a protocol.

## Where you have already met this

- Disk and phone encryption, which use AES, in modes designed for storage.
- HTTPS connections, whose bulk data is protected with AES-GCM or ChaCha20-Poly1305.
- Database fields that are encrypted by the application before they are stored.
- Security advisories about "nonce reuse" in a library or a protocol.
