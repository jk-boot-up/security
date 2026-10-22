# A Local Certificate Authority, Explained

## The idea in one sentence

A certificate authority is a signing policy plus a key: the policy decides what may be said, the name constraint limits the key's reach, and short lifetimes make taking a certificate back almost unnecessary.

## The 5 acts

### 1. Running a certificate authority

The root may certify one level below it and is kept offline. The issuing authority has path length zero (it cannot create more authorities) and a name constraint for `shop.internal`. A server certificate for `www.shop.internal` is issued for 90 days with the serverAuth purpose,
digital signature only, and it is not an authority. Requests with no name, 400 days or a name outside the domain are refused. Then the policy is skipped, as a buggy or stolen issuer would: the certificate is signed, but clients refuse it because of the name constraint.

### 2. Issuing from a signing request

payments-team signs a request with its own key. The request carries the public key only, so the authority never sees the private key, and the signature proves the sender holds it. The issued certificate carries exactly that public key. A request that claims
payments-team's public key but is signed by another key is refused, web-team may not ask for payments.shop.internal, and one forbidden name in a mixed request refuses the whole request. orders-team gets a client certificate (clientAuth) to call other services.

### 3. Revocation: taking a certificate back

payments.shop.internal's key is reported stolen on day 10, and serial 1002 goes into a list valid for a week. On day 11 a checking client refuses it and still accepts www; a client that never checks accepts the revoked certificate. On day 20, the day-10 list is past its next update:
the strict client refuses even the healthy certificate, failing closed, until a fresh list is fetched. Revocation only works if the authority publishes on time and every client fetches and checks.

### 4. Short-lived certificates instead of revocation

cart.shop.internal gets 24-hour certificates and a job that renews at two thirds of the lifetime, with a new key each time. Over 72 hours, checked every hour, there are 4 renewals and 0 failed checks. If the job stops, the certificate is refused as EXPIRED 25 hours after the last renewal,
with no list at all. A stolen 24-hour key works for at most a day. The inventory on day 65 flags the 90-day certificates as RENEW_NOW and the stopped one as EXPIRED: most certificate outages are expiries nobody saw coming.

### 5. The bill

Running an authority has costs. The root key signed three times in all (the issuing authority, itself and its revocation lists) and belongs offline, ideally in hardware. If the issuing authority's key is stolen, the offline root revokes it, and www.shop.internal and every certificate below it
are refused. The audit log holds every decision, 18 in the demo and 6 of them refusals. Every service also needs a request, a renewal job and a deployment step, which is why this is automated in practice with ACME.

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
