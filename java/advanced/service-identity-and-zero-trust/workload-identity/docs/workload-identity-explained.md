# Workload Identity, Explained

## The idea in one sentence

Give each workload a name it cannot choose and a certificate that expires in seconds, so a leak is worth a few seconds of one service.

## The 7 acts

### 1. One shared secret in three services

The input: 24 random bytes (never printed) copied into the configuration of payments, reporting and fraud-engine: 3 of 3 hold it. The ledger's rules: spiffe://bank.example/payments may POST /transfers, spiffe://bank.example/reporting may GET /statements.
The requests, each with the right key and an X-Service header that says who calls: reporting GET /statements 200, payments POST /transfers 200, reporting POST /transfers 403, and reporting saying payments POST /transfers 200: the ledger takes the caller's word.
The leak: fraud-engine's configuration reaches an attacker outside the bank. Saying payments, POST /transfers: 200; saying reporting, GET /statements: 200. A wrong key gets 401. With the one secret, the attacker is every service.

### 2. SPIRE decides who a workload is

The input: SPIRE server and agent 1.15.5 in one Linux container, trust domain bank.example, and 3 registration entries, each a selector and a SPIFFE ID: unix:uid:2001 -> spiffe://bank.example/ledger, unix:uid:2002 -> .../payments, unix:uid:2003 -> .../reporting.
The result: the same Java program, run as user 2001, 2002 and 2003, gets ledger, payments and reporting. Run as user 2003 with the claim "I am spiffe://bank.example/payments" in its environment, it gets spiffe://bank.example/reporting: the agent asks the kernel who the process is, and never reads the claim.
One SPIFFE ID, part by part: scheme spiffe://, trust domain bank.example, path /reporting.
How long an SVID lives: its own dates span 30 s (Not After - Not Before); SPIRE starts them 10 s early, so 30 - 10 = 20 s, the TTL; just issued, at most 20 s are left (yes).

### 3. Two Java services check each other's SPIFFE ID

The requests, over mutual TLS, with the SVIDs SPIRE issued: payments POST /transfers 200, payments GET /statements 403, reporting GET /statements 200, reporting POST /transfers 403. The ledger takes the caller's ID from the certificate, not from a header.
The ledger's audit: 4 requests, named spiffe://bank.example/payments and spiffe://bank.example/reporting.
Two impostors: a server run by the reporting workload, called by payments as if it were the ledger, is refused (payments accepts only spiffe://bank.example/ledger). A certificate the attacker made with keytool, naming itself spiffe://bank.example/payments, is refused at the handshake: it is not signed by the trust bundle, and 0 requests reach the ledger's code.

### 4. Rotation: an SVID that lives 20 seconds

The input: payments calls the ledger, a new connection each time, about 4 calls a second for 50 s, while SVIDs live 20 s: 50 / 20 = 2.5 lifetimes, so the certificates must be replaced during the test.
The result: at least 100 calls (yes), 0 failed; payments' own SVID replaced at least 3 times (yes), and the ledger's, as seen by payments, at least 3 times (yes), counted from the serial numbers seen one after the other. The services contain no renewal code: the Workload API client swaps the SVID and each TLS handshake reads the current one.

### 5. A stolen SVID

The theft: spire-agent api fetch writes payments' certificate, private key and bundle to disk; an attacker running as another user copies them.
The attacker's calls with the copy, POST /transfers: at once 200, and the ledger's audit names spiffe://bank.example/payments: a copy is as good as the original while it lives. After 21 s (more than the 20 s lifetime): refused.
At the same moment: payments itself, with a fresh SVID, gets 200; the leaked shared secret from the first act still gets 200.

### 6. A workload that matches no entry

The input: a process run as user 2004, which no registration entry selects, with the claim spiffe://bank.example/payments in its environment.
The result: it asks for an SVID: none issued; it calls the ledger: no SVID, so no handshake, and 0 requests reach the ledger's code.
Then the payments entry is deleted on the server. Within 40 s the agent stops issuing it: a new payments process gets no SVID. reporting, unaffected, GET /statements: 200. No entry, no identity.

### 7. Evicting the attacker

The shared secret is changed at the ledger: the leaked copy now gets 401, and 3 of 3 services fail until each is given the new secret: evicting one attacker is an outage and a rollout.
The stolen SVID: no service was changed (0); it expired by itself after 20 s. The leaked shared secret was still accepted (200) until it was changed.
A short lifetime is the revocation that always works; it costs a SPIRE server and agent that must stay up to renew.

## The verdict

Register each workload with a selector the platform decides, let the SPIRE agent renew its SVID, verify the peer's SPIFFE ID on both sides, authorize by that ID, and keep the lifetime shorter than you can detect a theft.

## How to recognise this in code you did not write

- An API key in an environment variable.
- A shared service account.
- A header such as X-Service that names the caller.
- A client certificate that lasts a year.

## Where you have already met this

- An API key in an environment variable copied into every service.
- A service account password nobody has changed since it was created.
- A client certificate that lives for a year.
- A header that says which service is calling.
