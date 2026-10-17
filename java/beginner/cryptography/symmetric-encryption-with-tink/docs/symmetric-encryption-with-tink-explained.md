# Symmetric Encryption with Google Tink, Explained

## The idea in one sentence

Google's Tink library makes encryption one safe call by choosing the nonce, the mode and the stored layout for you, keeping keys in a keyset with one primary key so they can be rotated, and leaving out unsafe options, while the keyset itself still needs a master key and a safe home.

## The 5 acts

### 1. One call, and no nonce to look after

Tink's Aead has one call to seal and one to open. The bank seals the line RENT -1200 twice, and the two results are different: Tink picked a new nonce each time, and the
caller never saw one. The message starts with five bytes, 01000003e9: the version, 1, and the key id, 1001, so Tink knows which key to open it with. The 16-byte line becomes
49 bytes: 5 for the key id, 12 for the nonce and 16 for the tag. Opened with the right key and account, the line comes back.

### 2. Any change is refused

Tink's messages are authenticated, as the plain version's were. Flip one bit of a sealed message and opening it fails. Move the message to another account, which changes the associated
data, and it fails. Open it with a different key that has the same key id, and it fails too, because the tag does not fit. In every case the caller gets an exception, and the demo reports a refusal.

### 3. Replacing the key without losing the data

Keys should be replaced from time to time. In Tink, the keyset holds several keys and one is primary. The bank adds a new key and makes it primary, so the keyset holds 2 keys. A message sealed before the
change still opens, because the old key is still in the keyset. A new message starts with a different key id. When the old key is later removed from the keyset, old messages can no longer be opened, but
new ones can. That is why re-encrypting old messages with the new key must happen before the old key is removed.

### 4. Keeping the keyset itself safe

The keyset holds the keys, so it is as secret as the data. Tink can write it encrypted under a master key, which in real use lives in a key management service or a hardware module and here is another
demo keyset. The file has an encryptedKeyset field, and the key's own bytes do not appear in it. Read back with the master key, it opens the old message. Read back with a different master key, it is refused.
This moves the problem one step: the master key now needs the safe home.

### 5. The bill: what Tink decides for you, and what it does not

Tink makes the safe choice the only one on the menu. Its AEAD templates include AES128_GCM, AES256_GCM, AES128_EAX, AES256_CTR_HMAC_SHA256, CHACHA20_POLY1305, XCHACHA20_POLY1305 and AES256_GCM_SIV,
and an ECB template is not offered. The costs are different from the plain project's. The stored format is Tink's own, so another library needs Tink, or knowledge of the layout, to read it. Tink adds a library and
its protocol buffer dependency. The master key still has to live somewhere. And message lengths still show: every message is 33 bytes more than its content.

## The verdict

Prefer a library such as Tink to hand-written AES-GCM. Use one `Aead` for each purpose, bind messages to context with associated data, rotate keys on a schedule and re-encrypt before removing old
ones, and store keysets encrypted under a master key that lives in a key management service. Know what the library does not do: it does not guard the master key, hide lengths, or make a lost keyset recoverable.

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
