# A Local Certificate Authority with step-ca, Explained

## The idea in one sentence

A local certificate authority is a service that decides which names get which certificates, for how long, and for whom, and clients trust it because they were given its root.

## The 5 acts

### 1. A certificate authority that runs

`step ca init` makes the root, Shop Internal CA Root CA: self-signed, valid 10 years, its private key in `secrets/root_ca_key`, encrypted with the CA's password. It also makes the issuing authority, Shop Internal CA Intermediate CA, signed by the root, with path length 0, so it may not create authorities.
The demo writes a name policy into `ca.json`: allowed names `localhost` and `*.shop.localhost`.
The CA answers over https. A client that trusts the root reads /health: ok. A client that was not given the root is refused (PKIX path building failed).

### 2. A certificate with a token

`step ca token www.shop.localhost` makes a one-time token. It is a JWT in 3 parts, header.payload.signature: header alg ES256, typ JWT; claims sub and sans www.shop.localhost, iss tokens (the provisioner that signed it), aud `<ca address>/1.0/sign`, exp - nbf 300 seconds, sha equal to the SHA-256 of the root, and a random 64-character jti.
With it, the service gets a certificate for [www.shop.localhost], 24 hours, key EC, a random serial, issued by the intermediate; a client that trusts the root accepts it.
The CA refuses the same token a second time (not authorized), a token for api.shop.localhost used for www (issued for another name), 2000 hours (longer than the longest lifetime) and pay.example (outside the policy).

### 3. A certificate with ACME

`step ca certificate --provisioner acme` asks for www.shop.localhost. The CA does not take a token: it calls the name back on port 80 (the challenge), which the step command answers, and only then signs.
The certificate is for [www.shop.localhost], 24 hours, key EC, a random serial, issued by the intermediate, and a client that trusts the root accepts it.
ACME for evil.example is refused: the ACME server will not issue for that name. ACME proves the asker controls a name now; the policy decides which names may be certified at all.

### 4. Renewing before it ends

Before the renewal, www.shop.localhost has a random serial, 24 hours and an EC key.
`step ca renew` presents the certificate and its key to the CA: renewed, 24 hours, a new serial true, the same key true, ends later true. `step ca rekey` renews with a new key: a different key true.
A service that has lost its key and tries with another one is refused: the key does not match, so the TLS call to the CA fails. Only the holder of the key renews. With a day of life, a renewal job that stops is noticed within a day.

### 5. Revocation, and the bill

`step ca revoke` revokes the acme certificate: revoked true, and a renewal afterwards is refused (not authorized).
The CA's revocation list, from GET /1.0/crl, is signed by Shop Internal CA Intermediate CA (true), has 1 entry, the acme serial (true), and is current for 5 minutes (next update - this update).
A client that checks the list refuses the acme certificate (REVOKED); a client that never checks accepts it; a client that wants to check and has no list refuses (UNDETERMINED_REVOCATION_STATUS), failing closed; www.shop.localhost against the same list is accepted.
The bill: the issuing key is online in `secrets/intermediate_ca_key` and the root key in `secrets/root_ca_key` on the same disk, both encrypted, so the machine and the password are what the whole trust rests on.

## The verdict

Keep certificates short so that renewal replaces revocation, restrict names with a policy and a name constraint, protect the issuing key like the crown jewel, give clients the root from a safe place, and make every service renew on its own.

## How to recognise this in code you did not write

- `step ca certificate`, `step ca renew` or `step ca revoke`.
- `acme` as a provisioner in `ca.json`.
- A `root_ca.crt` in a trust store.
- A systemd timer or a daemon that renews certificates.

## Where you have already met this

- `step ca certificate` or `step ca renew` in a script.
- `acme` as a provisioner name in a CA's configuration.
- A `root_ca.crt` copied into a trust store.
- A job that restarts a service after it renews a certificate.
