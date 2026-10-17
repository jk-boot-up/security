# Hashing and Integrity, Explained

## The idea in one sentence

A checksum catches accidental damage, a cryptographic hash gives data a fixed-size fingerprint that changes completely when the data changes, and only a keyed hash such as HMAC proves that data was made by a holder of a secret key and not changed since.

## The 5 acts

### 1. A checksum for accidents

Invoice INV-1001|ana|Laptop|1200 has a CRC-32 of a7c21715. If one bit flips on the disk, the CRC becomes d0c52783 and the damage is detected: a checksum
does its job. Now an attacker changes the amount to 12 and works out a new CRC, 5e22dd78, which they store beside the invoice. The check passes. A CRC has
no secret, so anyone can recompute it, and it guards against accidents only.

### 2. A cryptographic hash: SHA-256

SHA-256 is a cryptographic hash. The three letters abc give a hash that begins ba7816bf8f01, a published value. The invoice at 1200 dollars begins 878229813e30,
and at 1201 dollars begins 7da712a12a1c. Changing a single digit changes 62 of the 64 hex digits, which is 134 of the 256 bits: about half, which is what a good hash
gives for any change. And the hash is always 64 hex digits, for one line or for a whole book. It is a fingerprint: tiny change, completely different print.

### 3. A hash kept beside the data

Store a SHA-256 of each invoice right beside it. An attacker who changes the invoice to 12 dollars and leaves the hash is caught: the invoice no longer matches its hash.
But an attacker who can write the invoice can write the hash too, and the pair is consistent again: it looks intact. The hash protected nothing, because there is no secret
in it. A hash helps when it reaches the checker by a different road than the data. The shop publishes its installer's hash, 891124db5651, on its own website over HTTPS. A download
from a mirror hashes to 70fa24395df6, so it is refused. The mirror could change the file, but not the hash on the shop's own site.

### 4. A keyed hash: HMAC-SHA-256

HMAC mixes a secret key into the hash. The order service tags invoice INV-1001 with its key, and the tag begins 54916ce11158. The invoice with its tag is OK. An attacker who
changes the amount and keeps the tag gets BAD_TAG. One who tags it with a plain SHA-256 gets BAD_TAG. One who tags it with HMAC and a guessed key gets BAD_TAG. Without the key,
there is no way to make a tag the service accepts. The check also compares in constant time, so the first wrong byte is not revealed by how long it takes.

### 5. The bill: boundaries, repeats, and a shared key

HMAC is sound, and three things around it still go wrong. First, encoding. If the fields are joined with nothing between them, (ab, c) and (a, bc) give the same
tag, e120bc23dcb2, and an attacker moves a field boundary while keeping the tag: OK. Writing each field's length in front, the same change gets BAD_TAG. Second, repeats. A valid
invoice sent twice is accepted twice, because a tag does not say when it was made. When the service remembers invoice numbers, the second is REPLAYED. Third, the key. The warehouse
holds the same key in order to check tags, and so it can make one: a made-up invoice for 1 dollar is accepted. Key holders cannot be told apart. Proving who made a tag needs a
signature with a private key, in the next projects.

## The verdict

Use a CRC only for accidents, never a plain hash to protect against an attacker, and a hash only when it reaches the checker by another road. For data that an attacker could gain
by changing, use HMAC with a secret key, encode the fields unambiguously, give each message a number or a time that the receiver checks, compare in constant time, and use a
signature when the checker must not be able to make the tag.

## How to recognise this in code you did not write

- A SHA-256 checksum beside a download.
- An `X-Signature` or `X-Hub-Signature-256` header on a webhook.
- A JWT with the algorithm HS256.
- A comparison written with `MessageDigest.isEqual` or `hmac.compare_digest`.

## Where you have already met this

- SHA-256 checksums beside downloads on a project's website, so you can check a file you fetched.
- Git, whose commits are named by hashes of their contents.
- Webhook callbacks that arrive with an `X-Signature` header, which is an HMAC of the body.
- JWTs signed with HS256, which is HMAC with SHA-256.
