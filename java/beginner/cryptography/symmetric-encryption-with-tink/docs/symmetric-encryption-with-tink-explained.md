# Symmetric Encryption with Google Tink, Explained

## The idea in one sentence

Google's Tink library makes encryption one safe call by choosing the nonce, the mode and the stored layout for you, keeping keys in a keyset with one primary key so they can be rotated, and leaving out unsafe options, while the keyset itself still needs a master key and a safe home.

## The 6 acts

### 1. One call, and no nonce to look after

The bank gives Tink three things: the plain text RENT -1200 (16 bytes), the account account=ANA-1, which is checked but not hidden, and a keyset with one
AES256_GCM key, key id 1001. It seals the same line twice. Both messages are 49 bytes and start with the same five bytes, 01000003e9, but the two are not the same:
Tink picked a new random nonce each time, and the caller never saw one. Opened with the right key and account, the line comes back.

### 2. Inside one Tink message, worked out by hand

To see inside a message, the program builds one by hand with Java's own AES-GCM and a fixed nonce, 00000000 00000000 00000001 (only for the demo; Tink's nonces are random).
The 49 bytes are: the version, 01; the key id, 000003e9, because 1001 = 3 x 256 + 233 and 233 is e9 in hex; the 12-byte nonce; the 16-byte cipher text
9399a5a1 79f6597f b269750e 0efeb9b6; and the 16-byte tag fd69a8c7 c828ec0f c5f79504 11a6da99. 1 + 4 + 12 + 16 + 16 = 49. Tink opens the hand-built message, so this is exactly its layout.

### 3. Any change is refused

The message from act two ends with the tag fd69a8c7 c828ec0f c5f79504 11a6da99. Tink checks the tag before it returns anything. With the right key and account it opens.
Change the last byte from 99 to 98: refused. Move the message to account=MAL-2, which changes the associated data: refused. Open it with another key that has the same id, 1001: refused.

### 4. Replacing the key without losing the data

Keys should be replaced from time to time. Before rotation, the keyset holds key 1001, and a message sealed with it starts 01000003e9. The bank adds key 1002 and makes it primary:
the keyset holds key 1001 and key 1002, new messages start 01000003ea, and both old and new messages open. When key 1001 is removed, old messages are refused and new ones still open,
so old messages must be re-encrypted with the new key before the old key is removed. The demo uses a fixed new key id so the output repeats; real code uses a random one.

### 5. Keys that stay at home: envelope encryption

The keys have to live somewhere the application never reads. In envelope encryption each message gets a fresh 32-byte data key; the line is sealed with it, and only the data key goes to the
master key (key id 2001, standing in for a KMS) to be wrapped. A 16-byte line becomes 113 bytes: a 4-byte length, 00000041 = 65; the wrapped key, 65 = 5 + 12 + 32 + 16, starting 01000007d1;
and the cipher text, 44 = 12 + 16 + 16, with no key id. Two messages carry different wrapped keys; the master key was called 3 times, once per message, for two lines and a 100000-byte statement; another master key is refused,
and the master key's own bytes never appear in a message. Tink 1.24 deprecates writing out a whole secret keyset, so the project wraps data keys instead.

### 6. The bill: what Tink decides, and what it does not

Tink makes the safe choice the only one on the menu: its AEAD templates include AES128_GCM, AES256_GCM, CHACHA20_POLY1305 and XCHACHA20_POLY1305, among others, and an ECB template is not offered.
What it leaves to the bank: lengths still show: 16 bytes become 49 and 100000 become 100033, always 33 more (5 + 12 + 16); the stored format is Tink's own, so other libraries need Tink to read it;
it adds a library and its protocol buffer dependency; and the master key still has to live somewhere.

## The verdict

Prefer a library such as Tink to hand-written AES-GCM. Use one `Aead` for each purpose, bind messages to context with associated data, rotate keys on a schedule and re-encrypt before removing old
ones, and wrap data keys with a master key that lives in a key management service. Know what the library does not do: it does not guard the master key, hide lengths, or make a lost keyset recoverable.

## How to recognise this in code you did not write

- `KeysetHandle`, `Aead` and `AeadConfig.register()` in Java code.
- A stored value whose first five bytes are a version and a key id.
- A key management service that talks about a primary key version and older versions.
- A library with no function for choosing a nonce.

## Where you have already met this

- Tink's `Aead` in Android and server code at Google and elsewhere, and its cousins `DeterministicAead`, `Mac` and `PublicKeySign`.
- Key rotation in cloud key management services, which use the same primary and old keys idea.
- A stored value that begins with a small version byte and a key id, before the nonce.
- Libraries that do not let you pick ECB, or reuse a nonce, because the API has no place to put one.
