# Problem Statement

## The scenario

An online bank encrypts statements with an AES key. The key must reach the servers that need it, survive staff leaving and servers being rebuilt, and be replaceable, without being readable by anyone who should not have it.

## The naive version

Write the key in the source code, or in a file next to the program. It works on the first day, and it spreads the key to every copy and every reader of the program.

## What this project must deliver

- A demonstration that a hard-coded key is inside the compiled class.
- A key file with its permissions shown, and the limit of a file in the clear.
- A PKCS12 keystore that hides the key and its name, refuses a wrong password, and whose recorded protection settings are read from the file.
- Keys made with PBKDF2 and HKDF, checked against published values.
- Messages that carry the name of their key, and a rotation in which old messages open until the old key is removed.
- The same facts printed on every run, though keys, salts and nonces are random.
