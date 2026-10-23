# Crypto Agility, Explained

## The idea in one sentence

You can only change an algorithm you can find, and only in data that says how it was protected.

## The 5 acts

### 1. What the code asks for

A scan of the source for the names passed to Cipher, MessageDigest, Mac, Signature and the other getInstance calls finds 20 uses. The policy lists each name as approved, to migrate or forbidden. Forbidden ones, such as AES in ECB mode, MD5 and triple DES, are in 3 files.

### 2. A value that says how it was protected

10,000 records were written in AES-CBC with a MAC under key 1. The bank starts to write AES-GCM under key 2. Each value has a three-byte header with a magic byte, the suite version and the key id, and the header is authenticated with the data. The reader that picks the suite from the header reads all 10,100.

### 3. Moving the data

Retiring key 1 at the start is refused because 10,000 records depend on it. Reading 2,000 records with read-repair writes them again in the current suite: 8,000 remain on key 1. A batch moves them, and every record reads back as written. A second rotation to ChaCha20-Poly1305 under a third key moves all 10,100 again.

### 4. Retiring a key

After the second rotation the live records are all under key 3, so keys 1 and 2 can be retired. A backup of 100 records taken before the migration was sealed under key 1 and now reads 0 of 100: the backup must be migrated too, or the key kept as long as the backup.

### 5. Tampering and downgrade

The header is part of what is authenticated, so changing the version byte makes the check fail. A record that claims the first suite, which had no authentication at all, is refused by the policy. A version-1 record is refused after the policy stops reading version 1. The policy decides what is read and what is written.

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
