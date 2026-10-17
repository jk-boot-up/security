# Symmetric Encryption (AES-GCM), Explained

## The idea in one sentence

Symmetric encryption uses one secret key to lock and unlock data; AES in ECB mode leaks the pattern of repeated blocks, while AES-GCM hides the data and adds a tag that notices any change, as long as every message gets a nonce that is never reused with the same key.

## The 5 acts

### 1. A statement stored as it is

The bank stores each statement as plain text. A stolen disk gives the thief all eight lines of the statement, and the first line reads SALARY +4000.
There is nothing to decrypt. This is the starting point that encryption is meant to change.

### 2. AES in ECB mode: every block on its own

AES encrypts one 16-byte block at a time. Each line of the statement is exactly one block. In ECB mode, every block is encrypted on its own, so the same line always gives the same block of
cipher text. The statement has three different lines, a salary, the rent and a food bill, so the cipher text has three different blocks out of eight. The attacker cannot read any line,
but can see that five of the eight are the same payment, and where. The first and second rent blocks both begin 59a345b721951fa8. The pattern of the data shows through.

### 3. AES-GCM: hiding and checking

GCM mode uses a nonce, a number used once, for every message, so the same line sealed twice gives different cipher texts: 081a4d6d5de30f22 and e2d8337cdaa2332c. It also adds a 16-byte tag.
Opened with the right key and the right account, the line comes back. Flip one bit of the cipher text and it is refused. Move the message to another account, which
changes the associated data, the text that is bound to the message but not hidden, and it is refused. A different key is refused too. GCM both hides the data and notices
any change, which is what authenticated encryption means.

### 4. The one mistake: using a nonce twice

The one rule of GCM is that a nonce must never be used twice with the same key. Here it is. Two messages are sealed with the same key and the same nonce. The cipher text of the first XOR the cipher
text of the second equals the first plain text XOR the second plain text, because both were scrambled with the same stream of bytes. An attacker who knows the first message, balance=000100, XORs
that with the two cipher texts and gets the second: balance=900100. No key was needed. In GCM, a repeated nonce can also reveal the key used for the tags, so forgeries become possible too.

### 5. The bill: sizes, nonces, and the key

Encryption has costs. Five bytes seal to 33 bytes and 500 bytes to 528: always 28 more, 12 for the nonce and 16 for the tag, and the length still shows, so a short note and a long one can be told apart. With random
12-byte nonces, a repeat becomes likely near 4,294,967,296 messages under one key, so the key must be replaced long before that. And the key must be stored somewhere safe: a key kept beside the data protects
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
