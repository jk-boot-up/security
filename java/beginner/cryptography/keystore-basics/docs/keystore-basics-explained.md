# Keystores and Key Management Basics, Explained

## The idea in one sentence

A key should not live in the code or in a readable file but in an encrypted keystore opened by a password that is kept elsewhere, with separate derived keys for separate purposes, and with each message naming the key that sealed it so that keys can be replaced without losing data.

## The 5 acts

### 1. A key written in the source code

The first way to keep a key is to write it in the code. The bank compiles a program with the key inside it. The key's text is found inside the compiled class file, so anyone who has the program, a customer, a contractor
or a thief, has the key. It is also in the source repository, in every copy of the program and in every build, for good. And changing the key means building and shipping the program again.

### 2. A key in a file outside the program

Moving the key into a file outside the program means it is no longer in the jar. But a file is only as private as its permissions. A new file usually gets rw-r--r--, readable by
others on the machine: anyone with a login can read the key. With rw-------, only the owner can. The file still holds the key in the clear, so anyone who can read it, or a backup of it, has the key.

### 3. A PKCS12 keystore: an encrypted file of keys

A PKCS12 keystore is an encrypted file of keys. The key is saved in bank.p12 under the name statements-v1. The key's own bytes do not appear in the file, and neither does its name. Opened with the password, the key comes back. Opened with a wrong
password, it is refused. The password makes the key that protects the file, and the file records how many rounds that took: 10,000 by default. Raising the setting to 600,000 and saving again, the file records 10,000 in one place and 600,000 in another: the setting
reached one place and not another. So check what the file records, and not only the settings.

### 4. Making keys from other secrets

Keys can be made from other secrets. PBKDF2 turns a password and a salt into a 32-byte key, with 600,000 rounds so that guessing the password is slow. The same password and salt give the same key, and another salt gives another. HKDF turns one strong
master key into any number of separate keys: one for statements, a different one for login tokens, the same one each time for the same purpose. A key made from a weak password is only as strong as the password.

### 5. Replacing a key, and the bill

Keys must be replaced from time to time. A new key, statements-v2, is added to the keystore. Each message starts with the name of the key that sealed it, so the old message names statements-v1 and the new one names statements-v2, and both open while both keys are in
the keystore. After statements-v1 is removed, the old message is refused, its key is gone, so old messages must be sealed again with the new key before the old key goes. Then the bill: the keystore's password has to be kept somewhere too, a lost keystore file is lost data, and
the default rounds of the file are far below the 600,000 now recommended, so raise them and choose a strong password.

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
