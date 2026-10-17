# Digital Signatures (Ed25519), Explained

## The idea in one sentence

A digital signature is made with a private key and checked with a public key, so anyone can check it and only the key holder can make it; it should cover a fixed encoding of the message together with its purpose, version and expiry; and it is only as trustworthy as the public key it is checked against.

## The 5 acts

### 1. A tag made with a shared key

Start with HMAC, from the hashing project. The shop tags its price list, and the tag begins 2efbd273904d2215. The warehouse needs to check tags, so it holds the same key. That means the
warehouse can also tag a list of its own, with the laptop at 12 dollars, and the shop's check accepts it. Anyone who can check can forge. For a few services that fully trust each other that may be fine. For anything wider it is
not.

### 2. A signature: made with the private key, checked with the public key

A signature separates making from checking. The shop signs its price list with its private key, and the signature is 64 bytes, beginning a83b53e9ba4d832c. Signing the same list again gives the
same 64 bytes, because Ed25519 needs no random number. The warehouse holds only the public key and checks it: OK. If the laptop is changed to 12 dollars and the signature kept, the check says BAD_SIGNATURE. If Mallory signs her own
list with her own key, the check says BAD_SIGNATURE too. The warehouse can check, but has no key it could sign with for the shop.

### 3. What exactly is signed

A signature covers bytes, so the message must become bytes in one fixed way. The price list is written with its products sorted, one per line, so two lists with the same products built in a different order give the same bytes. And
a signature should be tied to its purpose. The shop signs the same bytes as a refund order, with the label shop.refund.v1. Checked as a refund order, that signature is fine. Offered as a price list, it is refused, because
the label is signed along with the message. A zero byte separates the label from the message, so one cannot run into the other. This is called domain separation.

### 4. A good signature on an old message

The shop published version 1, with the laptop at 1000, and then version 2, at 1200. An attacker replays the old version 1, which really was signed. A checkout that checks only the signature accepts it: OK. The
signature is perfect and the list is old. A checkout that remembers the newest version it has seen refuses it as OLD_VERSION. And a list that was only good until day 40, offered on day 50, is refused as EXPIRED.
The version and the expiry are part of the signed bytes, so nobody can change them without breaking the signature.

### 5. The bill: whose key is it, and what if it is stolen?

The check is only as good as the key. Mallory gives the checkout her own public key and says it is the shop's: her forged list verifies, OK. With the shop's real public key in the checkout, the same forgery is
refused. If the shop's private key is stolen, the thief's list is accepted until the key is revoked. A signature hides nothing: the price list is readable by anyone, signed or not. And the sizes: a signature is 64
bytes, a raw public key 32, and the same key in its encoded form 44. How to be sure a public key is the shop's is the question that certificates answer, in the next modules.

## The verdict

Use a signature, not an HMAC, whenever there are checkers who must not be able to make the message. Use Ed25519, or another modern scheme, from a vetted library. Sign an unambiguous encoding with a purpose label,
put a version and an expiry inside, and check them. Protect the private key, and get the public key to checkers by a route you can trust: a certificate, or a key pinned in the software.

## How to recognise this in code you did not write

- `Signature.getInstance("Ed25519")`, `initSign` and `initVerify`.
- A signature file beside a download, or a signed Git tag.
- A JWT with the algorithm `EdDSA`, `ES256` or `RS256`.
- A signed message that carries `version` and `notAfter` fields.

## Where you have already met this

- Signed software downloads and updates, and the signature files beside them (`.sig`, `.asc`).
- JWTs signed with RS256, ES256 or EdDSA, as opposed to HS256 which is HMAC.
- Git commits and tags that show "Verified" when signed.
- The certificate chain behind every HTTPS connection, which is signatures on signatures.
