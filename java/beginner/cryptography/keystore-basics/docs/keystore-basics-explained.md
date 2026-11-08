# Keystores and Key Management Basics, Explained

## The idea in one sentence

A key should not live in the code or in a readable file but in an encrypted keystore opened by a password that is kept elsewhere, with separate derived keys for separate purposes, and with each message naming the key that sealed it so that keys can be replaced without losing data.

## The 5 acts

### 1. A key written in the source code

The first way to keep a key is to write it in the code. Before compiling, the source holds KEY_TEXT = "hard-coded-statement-key-32by!". After compiling, the class file holds the same bytes,
68617264 2d636f64 65642d73 74617465 and so on, and the demo finds them there. So anyone who has the program, a customer, a contractor or a thief, has the key. It is also in the source repository and in every build, for good,
and a new key means building and shipping the program again.

### 2. A key in a file outside the program

Moving the key into a file means it is no longer in the program. But a file is only as private as its permissions. Before the fix, a new file gets rw-r--r--: read and write for the owner, read for the group,
read for others, so anyone with a login can read the key. After the fix, rw-------: only the owner. Either way the file holds the key in the clear, file-based-statement-key-32bytes, so anyone who can read it, or a backup of it, has the key.

### 3. A PKCS12 keystore: an encrypted file of keys

A PKCS12 keystore is an encrypted file of keys. Before saving: the name statements-v1, the key, and the password. After saving, the key's bytes are not in bank.p12, but its name is, as UTF-16 text, so a name is a label, not a secret.
Opened with the password, the key comes back; with a wrong password it is refused. The password is turned into the keys that protect the file, with rounds of PBKDF2, and the file records the rounds in two places: the lock on the key, made when the key is put,
and the integrity check, made when the file is saved. By default both are 10,000. Raising the setting only at save gives 10,000 and 600,000: the key's lock is unchanged. Setting the rounds when the key is put, too, gives 600,000 in both places.

### 4. Making keys from other secrets

Keys can be made from other secrets. PBKDF2 takes a password, correct horse battery staple, a salt, per-user-salt-1, and a number of rounds, 600,000. Round one is HMAC-SHA-256 of the salt followed by 00000001, with the password as the HMAC key: U1 starts 560d7b6f.
Each further round is the HMAC of the result before it: U2 starts 29bb6e9e. The key is all 600,000 results XORed together, and starts 29f87e05: the very key the JDK's PBKDF2 gives. So every guess at the password costs 600,000 HMACs.
Another salt gives another key. HKDF makes, from one strong master key, a separate key for each purpose: one for statements, another for login tokens, and the same one again when asked again. A key made from a weak password is only as strong as the password.

### 5. Replacing a key, and the bill

Keys must be replaced from time to time, so each stored message names its key. A message is 61 bytes: one byte for the length of the name (0d, 13), the name statements-v1, a 12-byte nonce, and 35 bytes of cipher text and tag (19 + 16).
Before statements-v1 is removed, with statements-v2 added, the old message names statements-v1 and the new one statements-v2, and both open. After statements-v1 is removed, the old message is refused, so old messages must be sealed again with the new key before the old key goes.
Then the bill: the keystore's password needs a home too, a lost keystore file is lost data, and the default 10,000 rounds are far below the 600,000 recommended.

## The verdict

Keep keys out of the code and out of repositories. Keep them in a keystore, or better in a key management service or hardware module that never lets them leave. Derive a key for each purpose from a strong master key. Put the key's name in
every message, replace keys on a schedule, re-seal old messages before removing an old key, and back up the keystore and its password separately. Check what the file records about its own protection.

## How to recognise this in code you did not write

- `KeyStore.getInstance("PKCS12")` and `keytool` in build and deployment scripts.
- A `.p12` file next to an application, and a password in its configuration.
- A message or token format that starts with a key id.
- Secret scanning tools that fail a build when a key is committed.

## Where you have already met this

- Java's `cacerts` file and `keytool`, which manage the certificates the JDK trusts.
- `.p12` and `.pfx` files for TLS keys, and Spring's `server.ssl.key-store`.
- Cloud key management services, and hardware security modules, which hold keys that never leave them.
- Breach reports that begin "an API key was committed to a public repository".
