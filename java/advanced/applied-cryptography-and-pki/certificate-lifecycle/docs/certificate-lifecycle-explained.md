# Certificate Lifecycle, Explained

## The idea in one sentence

A certificate fails when it is revoked without anyone asking or expires without anyone watching, so make the answer reach the client and make renewal automatic and watched.

## The 5 acts

### 1. Issue and read

The input: a root and an intermediate made with openssl. The root is self-signed with no path length limit; the intermediate is signed by the root with path length 0; both may sign certificates and CRLs. The root signs only the intermediate, and the intermediate signs every certificate, CRL and OCSP answer below.
The certificate: api.bank.example and payments.bank.example, digitalSignature with the extended usage serverAuth, not a CA, an EC key of 256 bits signed SHA256withECDSA, 30 days, with a pointer to the OCSP responder and no CRL distribution point.
How the chain is checked, by hand: 1, the leaf's issuer is the intermediate's subject; 2, the leaf's signature checks with the intermediate's key; 3 and 4, the same between the intermediate and the root; 5, the root is self-signed and held by the client; 6, the intermediate is a CA allowed to sign with path length 0; 7, the time is inside every certificate's dates. All seven hold.
The result: with the intermediate, accepted by hand and by the JDK. Without it, step 1 fails and the JDK finds no chain to the root. 31 days later and a day before issue, step 7 fails and the JDK says expired and not yet valid.

### 2. Revocation and a dead responder

The input: two policies for a client that cannot learn the status. Hard fail refuses the certificate; soft fail accepts it anyway.
OCSP, the responder up: the good certificate is accepted and the revoked one refused, by both clients; the responder saw 2 requests for the 2 good checks. Down or hanging: the hard-fail client refuses even the good certificate (status unknown) and the soft-fail client accepts the revoked one, 2 of 2 times. A hanging check waited about 1 second, the demo's timeout, against 15 by default.
A CRL: a signed list good for 3600 seconds in this lab, with 1 entry. api.bank.example is revoked after it; a client with that CRL accepts it, a client with the next CRL (2 entries) refuses it. Two hours later the next update has passed: hard fail refuses, soft fail accepts.
How big a CRL gets: 20 KB at 1,000 entries, 205 KB at 10,000 and 2120 KB at 100,000, which takes 17.4 seconds at 1 Mbit/s. One entry, read from the CRL: 2 bytes of header, 2 + 2 for the serial 1000 and 2 + 13 for the date, 21 bytes; the serial 1969F needs 3 bytes, so 22. With 90,000 of 100,000 entries already expired, openssl ca -updatedb keeps them (2120 KB); dropping them, as RFC 5280 allows, leaves 205 KB.

### 3. Stapling

The input: a staple is the OCSP answer, signed and dated, carried in the TLS handshake; it is under 512 bytes.
The responder up, a hard-fail client: the server that staples is accepted with 1 response in the handshake and 0 requests to the responder; the server that does not staple is accepted with 0 in the handshake and 1 request, so the responder sees which site the client visits.
The responder down: the server that does not staple is refused by the hard-fail client (status unknown) and accepted by the soft-fail one; the server that staples is accepted, because the answer came with it. Read 2 days later, past its next update, the staple is refused as out of date.
The certificate revoked: the old staple is still accepted, with 0 requests to the responder; a client that asks the responder refuses, and so does a staple made after the revocation. A staple is as old as its last refresh, 1 day in this lab.

### 4. Short lives and rotation

The input: step-ca issues ledger.bank.example with a lifetime of 12 seconds, as asked to within a second, and sends a chain of 2 certificates.
How the schedule is made: the renewal is due after two thirds of the life, 12 x 2/3 = 8 seconds; the headroom for the CA to be away is 12 - 8 = 4 seconds; the monitor alerts when a quarter of the life is left, 12 / 4 = 3 seconds. A stolen certificate is worth 12 seconds at most, not 30 days.
The result, rotation: in 30 seconds with a handshake every 100 ms, 300 handshakes and 0 failed, 3 renewals and 0 failed, 0 alerts, and the service now presents a serial that is not the one at the start.
The CA away for about 2 seconds, just before a renewal: 200 handshakes in 20 seconds, 0 failed; renewals failed while the CA was away and succeeded once it was back, inside the 4 seconds of headroom.

### 5. The missed renewal

The input: the CA stays down past the end of a 12-second certificate.
The result: before the expiry, 50 handshakes and 0 refused; after it, 100 of 100 refused with the reason certificate expired. More than 10 renewals failed and 0 succeeded. The monitor alerted before the first refusal, and at least 2 seconds before it.
The CA is back: renewing the expired certificate is refused, so it has to be issued anew; with the new one, 20 handshakes and 0 refused.
An inventory of 60 certificates, at the monitor's first scan: 4 expired, 6 under 7 days, 14 under 30 days and 36 with 30 days or more. The 4 expired ones are found before anyone reports them.
How an outage is counted over the next 60 days, with a scan every morning: the alert comes on the first morning with fewer than the alert's days left, the certificate is renewed the alert day plus the days the owner needs, and it is an outage when that is after its expiry. 26 expire: no monitor, 26 outages; a 7-day alert with 3 days needed, 3 outages and 23 prevented; with 10 days needed, 26 and 0; a 30-day alert with 10 days needed, 8 and 18.

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
