# Certificate Lifecycle, Explained

## The idea in one sentence

A certificate fails when it is revoked without anyone asking or expires without anyone watching, so make the answer reach the client and make renewal automatic and watched.

## The 5 acts

### 1. Issue and read

openssl makes the root and an intermediate with a path length of 0, and the intermediate signs a 30-day server certificate with two names, digitalSignature, serverAuth, CA false and a pointer to an OCSP responder. Java builds the chain with the intermediate and accepts it. Without the intermediate, 31 days later or a day before issue, it is refused, each for its own reason.

### 2. Revocation and a dead responder

With the responder up, a good certificate is accepted and a revoked one refused. With it down or hanging, a hard-fail client refuses both and a soft-fail client accepts both, including the revoked one. A client holding the old CRL accepts a certificate revoked after it was published, and a CRL past its next update is refused by a hard-fail client. A CRL of 1,000 entries is 20 KB; of 100,000 entries, 2120 KB.

### 3. Stapling

openssl s_server sends a response made by the intermediate. The Java client reads it from the handshake and accepts with 0 requests to the responder, even with the responder down. A staple made before a revocation is still accepted until it is refreshed; a client that asks the responder instead refuses at once; a staple made after the revocation is refused; a staple past its next update is not used.

### 4. Short lives and rotation

step-ca issues certificates that live 12 seconds. A renewer asks for a new one for the same key when 8 seconds have passed, and the service presents it to the next handshake. In 30 seconds there are 300 real TLS handshakes, 3 renewals and 0 failures. A CA that is away for 2 seconds just before a renewal costs 0 of 200 handshakes: the headroom is a third of the life.

### 5. The missed renewal

With the CA stopped, the renewer fails every attempt and the certificate expires: 50 handshakes before the expiry are accepted, and 100 of 100 after it are refused with certificate expired. A monitor that alerts at a quarter of the life left had alerted at least 2 seconds before the first refusal. The CA refuses to renew an expired certificate; a new one is issued and 20 of 20 handshakes pass. A monitor over 60 certificates finds 4 already expired, and a 7-day alert prevents 23 of 26 outages when the owner needs 3 days.

## The verdict

Prefer short-lived certificates renewed automatically, staple OCSP where revocation matters, choose hard or soft fail on purpose, and watch every certificate's expiry from an inventory with an alert early enough for its owner.

## How to recognise this in code you did not write

- A service down because a certificate expired on a weekend.
- A browser that skips the revocation check when the check fails.
- A CRL that has grown for years.
- A renewal job that fails quietly.

## Where you have already met this

- A certificate that expired on a weekend and took a service down.
- A browser that says the revocation check failed and carries on.
- A CRL download that takes longer than the page load.
- A spreadsheet of certificates and renewal dates.
