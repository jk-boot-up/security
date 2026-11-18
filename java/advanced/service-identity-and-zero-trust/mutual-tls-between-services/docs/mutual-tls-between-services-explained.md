# Mutual TLS Between Services, Explained

## The idea in one sentence

A certificate is a badge: check who it names, give it a short life, and renew it before it ends.

## The 5 acts

### 1. The identity in a certificate

The input: what step-ca issued. The ledger: CN=ledger with DNS localhost. The callers: CN=svc-payments-7 with spiffe://bank/payments, CN=svc-reporting-2 with spiffe://bank/reporting, CN=svc-fraud-engine-1 with spiffe://bank/fraud-engine.
The chain payments sends has 2 certificates. Checked by hand (ChainSteps): svc-payments-7 names Bank Internal CA Intermediate CA as its issuer, and that certificate's key verifies its signature; the intermediate names Bank Internal CA Root CA, which verifies it; that root is the 1 root in trust.pem. Trusted by hand: yes, the same answer as the JDK's own check.
How the ledger reads the identity: the subject CN=svc-payments-7 is not used; the SAN holds URI spiffe://bank/payments; removing the prefix spiffe://bank/ gives payments.
The key is EC, signed with SHA256withECDSA; notAfter - notBefore = 600 seconds = 10 minutes.

### 2. Who gets through the handshake

The input: three calls to POST /transfers. One has no client certificate. One has a certificate that names URI spiffe://bank/payments, but was issued by Partner Bank CA. The third is payments calling a ledger whose certificate names DNS ledger-old.bank.internal, while the client asked for localhost.
The result: the first is refused because the ledger wants a certificate; the second because its issuer is not in trust.pem; requests that reached the ledger's own code: 0. The third is refused by the client: the server's name does not fit. Mutual TLS checks both sides.

### 3. Allowed by identity, not by the network

The input: the ledger's rules. POST /transfers is for payments, and GET /statements is for reporting.
The result: payments POST /transfers 200 (done for payments) and GET /statements 403 (may not); reporting GET /statements 200 and POST /transfers 403; fraud-engine POST /transfers 403: its certificate is valid, but there is no rule for it.
The same two callers on one host, POST /transfers: allowed because they are on the network, 2 of 2; allowed by the identity in the certificate, 1 of 2.

### 4. Rotation: a certificate that lives 8 seconds

The input: payments' certificate, notAfter - notBefore = 8 seconds, accepted now; a caller keeps calling the ledger on new connections meanwhile.
The renewal, halfway through its life: step ca renew gives a new serial number and a later expiry, and the caller switches to it.
The server reloads without a restart: a renewed server certificate, which clients see; the roots in trust.pem go from 1 to 2 as Partner Bank CA is added, so its certificate that names payments goes from handshake refused to 200. Adding a CA is adding everyone it can mint.
The result: the first certificate after its expiry is refused at the handshake and 0 requests reach the ledger's code; the renewed certificate gets 200; at least 20 calls were made meanwhile, and 0 failed.

### 5. A stolen certificate, and revocation

The input: a copy of payments' certificate and key, used from another process. It calls POST /transfers: 200, because a certificate cannot tell who holds its key.
Revoked at the CA: step ca revoke puts its serial number on the CA's revocation list (CRL); the demo fetches it, checks its signature and finds 1 entry.
The result at the ledger: before the ledger reads the list, 200. After it reads the list, 403, certificate revoked. payments' own certificate, not revoked, still gets 200.
The bill: a revocation works only where the list is read; a short lifetime is the revocation that always works; every CA in trust.pem can mint any identity.

## The verdict

Use a private CA, give every service a certificate with its identity in the SAN, authorize by that identity, keep lifetimes short and renew automatically, make servers reload certificates and trust without a restart, and read the revocation list or rely on expiry. Treat the trust file as part of the policy.

## How to recognise this in code you did not write

- An internal API that trusts the source address.
- A client certificate checked for validity but never for who it names.
- A certificate renewal that is a calendar reminder.
- A revocation that nobody's server reads.

## Where you have already met this

- An internal API that trusts the source address.
- A client certificate checked for validity but never for who it names.
- A certificate renewal that is a calendar reminder.
- A revocation that nobody's server reads.
