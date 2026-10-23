# Workload Identity, Explained

## The idea in one sentence

Give each workload a name it cannot choose and a certificate that expires in seconds, so a leak is worth a few seconds of one service.

## The 6 acts

### 1. One shared secret

The ledger accepts one static key and a header that says which service is calling. Payments, reporting and fraud-engine hold the same key. When the fraud-engine's configuration leaks, an attacker who says payments moves money (200) and one who says reporting reads statements (200). Even a service that is not allowed to transfer says payments and gets 200.

### 2. Identity by registration

A SPIRE server and agent run in a Docker container with 3 registration entries, each chosen by a selector, the operating system user id. The Java program run as the ledger, payments and reporting users gets spiffe://bank.example/ledger, /payments and /reporting. A process that puts a claim to be payments in its environment still gets the identity of the user it runs as.

### 3. Two services check each other

Payments posts a transfer and gets 200, asks for statements and gets 403; reporting is the other way round. The ledger's audit names each caller by the SPIFFE ID in the certificate. Payments refuses a server that presents another workload's SVID, and the ledger refuses a certificate that names itself payments but was made by the attacker: 0 requests reach the ledger's code.

### 4. Rotation without a restart

Payments calls the ledger on new connections for 50 seconds. Its own SVID is replaced at least 3 times and the ledger's, as payments sees it, at least 3 times. At least 100 calls are made and 0 fail. The services have no renewal code: the Workload API client swaps the SVID and the TLS context reads it at each handshake.

### 5. A stolen SVID

The certificate, key and bundle on payments' disk are copied by an attacker running as another user and POST /transfers gets 200, and the ledger's audit names the call as payments. After the SVID's lifetime the same copy is refused while payments itself still gets 200. The leaked shared secret still gets 200 at that moment.

### 6. No entry, no identity

A process run as the attacker's user, saying it is payments, gets no SVID and 0 of its requests reach the ledger. When the payments entry is deleted, a new payments process gets no SVID within 40 seconds while reporting still gets 200. Changing the shared secret to evict an attacker would have broken 3 of 3 services.

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
