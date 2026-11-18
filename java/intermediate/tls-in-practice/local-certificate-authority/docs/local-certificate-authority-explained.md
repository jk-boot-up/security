# A Local Certificate Authority, Explained

## The idea in one sentence

A certificate authority is a signing policy plus a key: the policy decides what may be said, the name constraint limits the key's reach, and short lifetimes make taking a certificate back almost unnecessary.

## The 5 acts

### 1. Running a certificate authority

The root (serial 1, valid twenty years) may certify one level below it and is kept offline. The issuing authority (serial 2, five years) has path length 0, so it cannot create more authorities, and a name constraint: names only under `shop.internal`.
web-team asks for www.shop.internal for 90 days. The certificate is serial 1000, valid 2026-01-05 09:00 to 2026-04-05 09:00, serverAuth, digital signature only, and not an authority.
A client checks the chain by hand in the JDK's order, from the root down: the trusted root, the issuing CA's signature and dates, the name constraint, the path length, the leaf's signature and dates. All seven are true, and `ChainSteps` reaches the same verdict as the JDK's validator: accepted.
Requests with no name, 400 days or a name outside the domain are refused. Then the policy is skipped, as a buggy or stolen issuer would, and pay.example is signed as serial 1001; the client's step 4 is false and both the hand check and the JDK say refused (INVALID_NAME).

### 2. Issuing from a signing request

payments-team makes a key pair and signs a request for payments.shop.internal with its private key. The request carries the names and the public key (EC, 256-bit curve); the private key stays with the team.
The authority checks, in order: the signature verifies with the request's own key (proof of possession), there is at least one name, the name is inside shop.internal, payments-team may ask for it, and 90 days is within the server profile. All true, so it signs serial 1002, whose public key is exactly the request's.
A request carrying payments-team's public key but signed by another key is refused: not signed by the key it carries. web-team asking for payments.shop.internal is refused, and one forbidden name refuses a mixed request. orders-team gets serial 1003 for clientAuth, 30 days, and the chain check accepts it.

### 3. Revocation: taking a certificate back

payments.shop.internal's key is reported stolen on day 10. The issuing CA signs a revocation list: this update 2026-01-15 (day 10), next update 2026-01-22 (day 17), revoked serial 1002 for key compromise. The root's own list (day 0 to day 365) revokes nothing, so it covers the issuing CA.
On day 11 a client that checks the lists refuses payments (REVOKED) and accepts www; a client that never checks still accepts the revoked certificate.
On day 20 the day-10 list is past its next update. A strict client refuses even the healthy www certificate (UNDETERMINED_REVOCATION_STATUS): with no current list it fails closed. With a fresh list from day 20 it accepts again.

### 4. Short-lived certificates instead of revocation

cart.shop.internal gets serial 1004, valid 2026-02-04 09:00 to 2026-02-05 09:00. The renewal time is worked out from the certificate itself: a lifetime of 24 hours, two thirds is 24 x 2 / 3 = 16 hours, so it renews at 2026-02-05 01:00.
Over 72 hours, checked every hour, it renews at hours 16, 32, 48 and 64: 5 certificates, 5 different keys, 0 failed checks.
The job stops. The last certificate, serial 1008, ends 2026-02-08 01:00; 25 hours after it began it is refused (EXPIRED), with no list at all.
The inventory on day 65 flags the two 90-day certificates as RENEW_NOW with 25 days left, and the stopped cart certificate as EXPIRED. A stolen 24-hour key works for at most a day; a stolen 90-day key, for up to 90 days unless every client checks.

### 5. The bill

The root key signed four times: its own certificate, the issuing authority's certificate, its revocation list of day 0, and on day 40 a list that revokes the issuing CA. Each signature is a ceremony with an offline key, ideally in hardware.
The day-40 list revokes serial 2 (Shop Issuing CA) for CA compromise, so on day 41 www.shop.internal is refused (REVOKED), and so is every certificate below the issuing CA.
The audit log holds 18 decisions, 6 of them refusals, each with its reason. Every service still needs a request, a renewal job and a deployment step: ACME automates them, as the -with-step-ca version shows.

## The verdict

Keep the root offline, issue from a constrained authority, apply the policy in the issuer, and prefer short-lived certificates renewed by automation over revocation lists that clients may not check. Use ACME and a maintained CA such as step-ca rather than your own code in production.

## How to recognise this in code you did not write

- `basicConstraints: CA:TRUE, pathlen:0` in an intermediate certificate.
- `X509v3 Name Constraints: Permitted: DNS:...` in `openssl x509 -text`.
- `CRL Distribution Points` and `Authority Information Access` in a certificate.
- A certificate-expiry outage in an incident report.

## Where you have already met this

- `openssl ca` and `openssl req` for signing requests.
- `keytool -genkeypair` and `-gencert`.
- The Subject Alternative Name list in a browser's certificate viewer.
- "CRL" and "OCSP" in a certificate's details, and the 90-day certificates of Let's Encrypt.
