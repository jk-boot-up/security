# Zero Trust Access, Explained

## The idea in one sentence

Inside the network proves nothing: check who, check what they may do, and write it down on every request.

## The 5 acts

### 1. Before: the ledger trusts the network

The input: ten requests, none with an identity: 3 balance reads, 3 transfers, 2 audit exports and 2 account freezes.
The result: sent straight to the ledger from inside the network, 10 of 10 succeed, the ledger's code runs 10 times, 3 of them transfers. The ledger sees an address, not a person.
The parts of the after: Keycloak 26.8.1 imports the realm bank (alice the teller, bob the auditor, carol the admin), OPA 1.21.3 runs policy/ledger.rego (its own tests: 15 of 15 pass), the proxy learns 1 signing key from the JWKS, and the device posture is a claim named compliant, set per Keycloak client.

### 2. Who is calling: the token, checked by hand

The input: alice's token from Keycloak, three base64url parts. The header says RS256 and names a key in the JWKS; the claims say issuer .../realms/bank, audience ledger-api, user alice, roles [teller], compliant true, and exp - iat = 300 seconds.
How the signature is checked: the key is RSA 2048 bits with e = 65537. The signature s is 256 bytes; m = s^e mod n is 256 bytes and must be 00 01, 202 bytes of FF, 00, the 19-byte label that says a SHA-256 hash follows, then 32 bytes equal to the SHA-256 of header.claims. They are, and the JDK's SHA256withRSA agrees. Then issuer, audience and expiry are checked.
Two forgeries: bob's token with the roles edited to admin keeps bob's real signature, so the padding is right but the hash is not; mallory's token signed with her own key gives m that is noise, with no padding.
The result at the proxy: no token, an expired token, a token for reports-api, the edited roles, a changed signature character, mallory's own key and the algorithm none all get 401. The same ten requests with no identity get 0 of 10 through the proxy, and 0 of 10 straight to the ledger, which wants the proxy's mark.

### 3. May they: OPA decides

The input: what the proxy sends OPA for alice's transfer: user alice, roles [teller], device_compliant true, method POST, path /transfers, hour 10, then the same at hour 23.
How the decision is made: OPA's own values for each rule. resource is transfer; role_ok is true because {POST, transfer, teller} is in the grants; device_ok is true; business_hours is true at 10:00 and undefined at 23:00. The reasons are [] at 10:00 and [outside business hours] at 23:00, and allow is count(reasons) == 0: true, then false.
The grants: a teller may GET balance and POST transfer, an auditor GET audit and GET balance, an admin GET audit, GET balance and POST freeze. Anything else is denied.
The result at the proxy: 11 requests with valid tokens, 200 for 5 and 403 for 6 (outside business hours, role not allowed three times, unknown operation, device not compliant). The ledger ran 5 times, only for the allowed ones.

### 4. A token outlives its session

The input: alice's token with a lifetime of 300 seconds; it gets 200 before her logout.
The result after the logout at Keycloak: the same token still gets 200, because its signature and expiry are still good. With a session check at Keycloak on every request it gets 401, session ended, while a new login and bob's token get 200; the check cost 3 calls to Keycloak for 3 requests.
A short lifetime instead: a 4 second token gets 200 before the logout and at once after it, and 401, expired, once its 4 seconds are over. The window after a logout is 4 seconds at most, not 300.
When a part is down: with Keycloak stopped a valid token still gets 200, because the keys are cached; with the session check on it gets 401, because the session cannot be confirmed. With OPA stopped the proxy answers 503, decision unavailable: no decision is a no.

### 5. One record per request

The input: one record, a line of JSON in build/lab/audit.log: seq 20, decision deny, status 403, reason outside business hours, user alice, method POST, path /transfers and the time.
The result: the demo sent 39 requests to the proxy and 39 records were written; all 39 match the request in order, with the status the caller saw.
The reasons: allowed 12, no token 11, bad signature 3, role not allowed 3, expired 2, session ended 2, and one each of decision unavailable, device not compliant, outside business hours, unknown operation, unsupported algorithm and wrong audience.
What the log does not hold: 0 of the 26 access and refresh tokens; and no name taken from a forged token, whose claims are never trusted: 23 of 39 records name a user.

## The verdict

Put the check at the resource: validate the token with the issuer's keys, decide with a default-deny policy, log one record per request without the token, keep token lifetimes short and add a session check where revocation must be quick, and make the resource refuse anything that skips the proxy.

## How to recognise this in code you did not write

- A service reachable by every pod because it is internal.
- A JWT whose payload is trusted before the signature is checked.
- An access log that cannot name a person.
- A logout button that leaves the token working.

## Where you have already met this

- A VPN that puts a laptop on the same flat network as the databases.
- An internal admin page with no login because it is internal.
- A role check copied into five services, each slightly different.
- An access log with IP addresses and no names.
