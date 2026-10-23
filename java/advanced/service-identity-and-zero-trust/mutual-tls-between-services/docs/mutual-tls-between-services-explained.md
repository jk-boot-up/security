# Mutual TLS Between Services, Explained

## The idea in one sentence

A certificate is a badge: check who it names, give it a short life, and renew it before it ends.

## The 5 acts

### 1. Who gets in

step-ca issues a certificate to each service, with the identity as a URI in the SAN and a different subject name. The ledger demands a client certificate. A call with none fails, and so does a call with a certificate from another CA that claims to be payments: the ledger's code runs 0 times. The client checks the server too: a ledger that shows a certificate for another host is refused.

### 2. Identity, not the network

The ledger reads the identity from the SAN. payments gets 200 on POST /transfers and 403 on GET /statements; reporting gets 200 on GET /statements and 403 on POST /transfers; fraud-engine has a valid certificate and no rule, so 403. A rule that allows the trusted network lets both of two callers on the same host in; the certificate identity lets one.

### 3. Rotation

A caller's certificate lives 8 seconds. Halfway through, step ca renew gives a new serial number and a later expiry; the server reloads its own certificate and its trust file without a restart, and a second CA added to the trust file is accepted at once. At least 20 calls on new connections run meanwhile, with 0 failures. After 8 seconds the first certificate is refused and the renewed one gets 200.

### 4. A compromised certificate

A copy of a certificate and its key works from any process: 200. The CA revokes it and lists it on its revocation list, but the ledger still answers 200 because nothing told it. Once the ledger loads the list it answers 403 with the reason. A certificate that was not revoked is unaffected.

### 5. The bill

The CA records a revocation but a server that does not read the list never learns of it. A short lifetime is the revocation that always works, and it costs a renewal job that must not fail. Adding a CA to the trust file lets it mint any identity, payments included: the trust file is part of the policy.

## The verdict

Use a private CA, give every service a certificate with its identity in the SAN, authorize by that identity, keep lifetimes short and renew automatically, make servers reload certificates and trust without a restart, and read the revocation list or rely on expiry. Treat the trust file as part of the policy.

## How to recognise this in code you did not write

- An internal API that trusts the source address.
- A client certificate checked for validity but never for who it names.
- A certificate renewal that is a calendar reminder.
- A revocation that nobody's server reads.

## Where you have already met this

- An internal API that trusts anything from the office network.
- A shared API key in every service's configuration.
- A certificate that expired on a Sunday and stopped payments.
- A firewall rule named after a subnet that nobody can explain.
