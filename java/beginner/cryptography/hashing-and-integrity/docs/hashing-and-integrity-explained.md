# Hashing and Integrity, Explained

## The idea in one sentence

A checksum catches accidental damage, a cryptographic hash gives data a fixed-size fingerprint that changes completely when the data changes, and only a keyed hash such as HMAC proves that data was made by a holder of a secret key and not changed since.

## The 6 acts

### 1. A checksum for accidents

Before the change, invoice INV-1001|ana|Laptop|1200 is stored with its CRC-32, a7c21715. After an accident, one bit flips on the disk, the last 0 becomes a 1, and
the CRC is d0c52783: it no longer matches, so the damage is detected. After an attack, the amount is 12 and the attacker stores the new CRC, 5e22dd78, beside it:
it matches, and the check passes. A CRC has no secret, so anyone can recompute it; it guards against accidents only.

### 2. A cryptographic hash: SHA-256

SHA-256 is a cryptographic hash. The three letters abc give the published value that begins ba7816bf8f01, so the code is checked against the standard. Before hashing,
two invoices differ in one digit: 1200 and 1201. After hashing, their hashes, 878229813e30... and 7da712a12a1c..., differ in 62 of the 64 hex digits, which is 134 of the
256 bits: about half, which is what a good hash gives for any change. And the hash is always 64 hex digits, for one line or for a whole book.

### 3. A hash kept beside the data

The store keeps a SHA-256 of each invoice right beside it. After attack 1, the invoice says 12 dollars and the old hash is left: they no longer match, and the change is caught.
After attack 2, the attacker, who can write the invoice, writes its new hash 242683b46b4c... too: the pair matches, and nothing is caught. The hash protected nothing, because there
is no secret in it. A hash helps when it reaches the checker by another road. The shop publishes its installer's hash, 891124db5651..., on its own site over HTTPS. A copy from a
mirror hashes to 70fa24395df6..., so it is refused. The mirror can change the file, but not the hash on the shop's site.

### 4. A keyed hash: HMAC-SHA-256

HMAC mixes a secret key into the hash. Before tagging, there are two inputs: the key, and the invoice as bytes, each field with its length in front (00000008 and the eight
letters INV-1001, then 00000003 and ana, and so on: 37 bytes). After tagging, the tag is 32 bytes and begins 54916ce11158. The invoice with its tag is OK. An attacker who changes
the amount and keeps the tag gets BAD_TAG; one who uses a plain SHA-256 as the tag gets BAD_TAG; one who makes an HMAC with a guessed key gets BAD_TAG. The check compares in
constant time, so the first wrong byte is not revealed by how long it takes.

### 5. How the tag is made, step by step

HmacSteps redoes HMAC-SHA-256 by hand. The key, 64656d6f... (the letters demo-...), is padded with zeros to 64 bytes, the block size of SHA-256. Step 1: every byte XOR 36 gives
the inner key, 52535b59.... Step 2: SHA-256 of the inner key followed by the invoice bytes gives the inner hash, 5bd27bbf.... Step 3: every key byte XOR 5c gives the outer key,
38393133.... Step 4: SHA-256 of the outer key followed by the inner hash gives the tag, 54916ce1 1158db43..., exactly the tag Java's Mac gave in act four. The key goes in twice,
so neither hash can be made without it.

### 6. The bill: boundaries, repeats, and a shared key

HMAC is sound, and three things around it still go wrong. First, boundaries. Before the fix, the fields are joined with nothing between them: (ab, c) and (a, bc) are the same
bytes, INV-7abc5, so the same tag, e120bc23dcb2, and a moved boundary is OK. After the fix, each field has its length in front, the tags differ (dd8b59649a21 and 397d08fb696a),
and the same trick gets BAD_TAG. Second, repeats. A tag has no date, so a valid invoice sent twice is accepted twice, until the service remembers invoice numbers: OK, REPLAYED.
Third, the key. The warehouse holds the same key to check tags, so it can make one: a made-up invoice for 1 dollar is OK. Key holders look alike; proving who made a tag needs a
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
