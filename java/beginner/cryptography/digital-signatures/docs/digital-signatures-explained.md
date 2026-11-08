# Digital Signatures (Ed25519), Explained

## The idea in one sentence

A digital signature is made with a private key and checked with a public key, so anyone can check it and only the key holder can make it; it should cover a fixed encoding of the message together with its purpose, version and expiry; and it is only as trustworthy as the public key it is checked against.

## The 6 acts

### 1. A tag made with a shared key

Start with HMAC, from the hashing project. Before tagging there are two inputs: the price list (version=2 validUntilDay=100 laptop=1200 mouse=25) and a shared key. After tagging
there is a 32-byte tag, beginning 2efbd273 904d2215. The warehouse needs to check tags, so it holds the same key. That means the warehouse can also tag a list of its own, with the
laptop at 12 dollars, and the shop's check accepts it. Anyone who can check can forge.

### 2. A signature: the private key signs, the public key checks

A signature separates making from checking. Before signing there are three inputs: the label shop.price-list.v1, the price list, and the shop's private key, which only the shop has.
After signing there is a 64-byte signature, beginning a83b53e9 ba4d832c. Signing the same list again gives the same 64 bytes, because Ed25519 needs no random number. The warehouse
holds only the public key, beginning d75a9801, and checks: the real list is OK; the laptop changed to 12 dollars with the signature kept is BAD_SIGNATURE; a list Mallory signed with
her own key is BAD_SIGNATURE. The warehouse can check, but has no key it could sign with for the shop.

### 3. How the signature is made, step by step

Ed25519 uses one fixed point B on a curve. "Times B" means adding B to itself that many times: quick to do, and nobody knows how to undo it. Step 1: hash the private key with
SHA-512 and cut the 64 bytes in two; the first half, with three bits fixed, is the secret number a (307c8386 ...), the second half is a prefix (9b4f0afe ...), and the public key is
A = a x B (d75a9801 ..., the shop's public key). Step 2: r = SHA-512(prefix + message) mod L, where "mod L" keeps the remainder after dividing by a fixed large number L; the same
message always gives the same r, so no random number is needed. Step 3: R = r x B (a83b53e9 ...), the first 32 bytes of the signature. Step 4: k = SHA-512(R + A + message) mod L.
Step 5: S = (r + k x a) mod L (193b6f5a ...), the last 32 bytes. R + S is exactly the signature Java made. To check, the warehouse works out S x B and R + k x A: equal for the real list,
not equal once the mouse is changed to 24. Only someone who knows a can make an S that passes.

### 4. What exactly is signed

A signature covers bytes, so the message must become bytes in one fixed way. Before signing, three parts are joined: the label shop.price-list.v1, a zero byte, and the price list,
one field per line with the products sorted by name, so two lists built in a different order give the same bytes. Around the zero byte the bytes are 742e7631 ("t.v1"), 00, then
76657273 ("vers"). The shop then signs the same list for another purpose, a refund order, with the label shop.refund.v1. Checked as a refund order, the signature is fine; offered as a
price list, it is refused, because the label is signed along with the message. The zero byte keeps the label from running into the message. This is called domain separation.

### 5. A good signature on an old message

Before: the shop signed version 1, with the laptop at 1000, and then version 2, at 1200; both signatures are genuine. The attack: on day 50 an attacker replays version 1. A checkout
that checks only the signature accepts it: OK. The signature is perfect and the list is old. After the fix: a checkout that remembers the newest version it has seen refuses it as
OLD_VERSION, and a list that was only good until day 40, offered on day 50, is refused as EXPIRED. The version and the expiry are part of the signed bytes, so nobody can change them
without breaking the signature.

### 6. The bill: whose key is it, and what if it is stolen?

The check is only as good as the key. The attack: Mallory signs a list with the laptop at 1 dollar with her own key, gives the checkout her public key and says it is the shop's: her
list verifies, OK. After the fix, with the shop's real public key in the checkout, the same list is BAD_SIGNATURE. What a signature cannot do: stop a thief with the shop's private key,
whose list is accepted until the key is revoked; or hide anything, since the price list is readable by anyone. The sizes: a signature is 64 bytes, a raw public key 32, and the same key
in its encoded form 44. How to be sure a public key is the shop's is the question that certificates answer, in the next modules.

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
