# Zero Trust Access, Explained

## The idea in one sentence

Inside the network proves nothing: check who, check what they may do, and write it down on every request.

## The 5 acts

### 1. The perimeter

The ledger API trusts the network. Ten requests with no identity (3 balances, 3 transfers, 2 audit exports, 2 freezes) all succeed, its code runs 10 times and 3 transfers are executed for callers nobody can name. The ledger can log an address, not a person.

### 2. Who is calling

The proxy checks the signature against the keys Keycloak publishes, the issuer, the audience and the expiry, and accepts only RS256. No token, an expired token, a token for another audience, a token with its roles edited to admin, a flipped signature character, a token signed with the attacker's own key and a token with the algorithm none all get 401. The ten identity-less requests get 0 successes, and 0 when sent straight to the ledger.

### 3. May they

With a good token, the proxy asks OPA with the user, roles, device posture claim, method, path and hour. Bob the auditor cannot transfer, Alice the teller cannot read the audit, Carol the admin is not a teller, a device that is not compliant is refused, and a teller's transfer at 23:00 is refused while the same transfer at 10:00 gets 200. The Rego file has 15 of 15 tests passing.

### 4. Logged

Each request ends in one place that writes one record: allow or deny, the status, the reason, the user, the method and the path. Of 39 requests sent, 39 records match in order with the status the caller saw: 12 allow and 27 deny, each with a reason. None of the 22 tokens used appears in the log.

### 5. A token outlives its session

Alice's session is ended at Keycloak, and her token, good for 300 seconds, still gets 200: the proxy checks signature and expiry. With a session check at Keycloak on every request the same token gets 401, at a cost of one call per request (3 for 3). A token with a 4 second lifetime is accepted at once after its session ends and refused after 4 seconds. With Keycloak stopped, old tokens still work without the check and are refused with it; with OPA stopped the answer is 503.

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
