# Crypto Agility, Explained

## The idea in one sentence

You can only change an algorithm you can find, and only in data that says how it was protected.

## The 5 acts

### 1. What the code asks for

The input is the bank's source: ten files, and in each the names passed to Cipher, MessageDigest, Mac, Signature, SecureRandom and the other getInstance calls.
The result is the inventory: each of the 13 different names with how often it is used and what the policy says. AES-GCM, ChaCha20-Poly1305, SHA-256 and HMAC-SHA256 are approved; AES-CBC, RSA with PKCS #1 padding, SHA-1 and SHA1PRNG are to migrate; AES in ECB mode, MD5 and triple DES are forbidden.
Counted: 20 uses in 10 files, 11 approved, 5 to migrate, 4 forbidden, 0 not on the list. The files with a forbidden name are fixed first.

### 2. A value that says how it was protected

The input is one record, customer-1;balance=37, 21 bytes of text. Sealed in each suite it becomes header + IV + ciphertext + tag: 3 + 16 + 32 + 32 = 83 bytes in AES-CBC with HMAC-SHA256 (CBC pads 21 bytes to 32), and 3 + 12 + 21 + 16 = 52 bytes in AES-GCM and in ChaCha20-Poly1305.
The header is the letter E (hex 45), then the version (01, 02, 03) and the key id. The tag covers the header too, so it cannot be changed unseen.
The store holds 10,000 records written by the old system as v1/key1. The bank starts writing AES-GCM under key 2: 100 new records are v2/key2. The old reader, which knows only version 1, refuses the new record with header 45 02 02; the reader that picks the suite from the header reads 10,100 of 10,100.

### 3. Moving the data

The input is the store: 10,000 records as v1/key1 and 100 as v2/key2. Retiring key 1 now would strand 10,000 records, so it is refused.
Step 1 is read-repair: 2,000 reads each write the record again in the current suite and key. The result is 8,000 on v1/key1 and 2,100 on v2/key2.
Step 2 is a batch for the rest: it changes 8,000 records, and all 10,100 are v2/key2.
How the counts add up: key 1 goes 10,000 - 2,000 = 8,000, then - 8,000 = 0; key 2 goes 100 + 2,000 = 2,100, then + 8,000 = 10,100. All 10,100 read back as the text that was put in.

### 4. The next rotation, and retiring a key

The input is the store after the first move: 10,100 records as v2/key2. A batch to ChaCha20-Poly1305 under key 3 changes all 10,100, and they all read back as written. The process is the same as the first time.
Live records per key: key 1 has 0, key 2 has 0, key 3 has 10,100, so retiring keys 1 and 2 is allowed.
But a backup of 100 records was taken before the first move, with header 45 01 01 (v1/key1). While key 1 lives it reads 100 of 100. After key 1 is retired it reads 0 of 100: "key 1 is retired". A backup must be migrated too, or the key kept as long as the backup.

### 5. Tampering and downgrade

The input is record 5 as stored: header 45 03 03, 53 bytes, and it reads. The policy reads v1, v2 and v3 and writes v3; after the cut-off it reads v2 and v3 only.
Three changes are made before the read: the version byte becomes 02 (claims AES-GCM); the version byte becomes 00 (claims AES in ECB mode, which has no tag); and a version-1 record (header 45 01 04) is read after the cut-off.
The result: the first is refused because the tag does not match, since the tag covers the header. The second is refused because version 0 is not accepted by the policy. The third is refused because version 1 is not accepted by the policy any more.

## The verdict

Keep an inventory of algorithm names, put the suite and key id in an authenticated header, write the newest suite and read only what the policy allows, rewrite on read and by batch, and retire a key only when the records and the backups under it are gone.

## How to recognise this in code you did not write

- Ciphertext in a column with no version.
- An algorithm name hard-coded in many places.
- A key kept for ever because nobody knows what it protects.
- A migration that was never rehearsed.

## Where you have already met this

- A column of ciphertext with no version.
- An algorithm name hard-coded in forty places.
- A key kept forever because a backup needs it.
- A migration script run once, in a hurry, and never again.
