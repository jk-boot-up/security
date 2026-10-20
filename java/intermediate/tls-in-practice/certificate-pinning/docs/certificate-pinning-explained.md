# Certificate Pinning and What It Costs, Explained

## The idea in one sentence

A pin narrows trust from every authority on the device to a key you chose, so it stops interception by a trusted-but-unexpected root, and costs a planned rotation, backup pins and an update path.

## The 5 acts

### 1. An app that trusts the device

The device trusts the public root and one more root, installed by a company proxy. An app that only validates the chain accepts the genuine server, and accepts the proxy's certificate for api.bank.example too: both chains are valid for the device's trust store.

### 2. An app that pins the server's key

The app ships the SPKI hash of the server's leaf key. The genuine server's chain contains that key and is accepted. The interceptor's chain validates against the extra root, but contains no pinned key, so the app refuses it by the pin. On a device without the extra root the same chain
never gets as far as the pin check: validation refuses it first. A pin adds a condition to validation and never replaces it.

### 3. The server renews with a new key

On day 80 the server renews its certificate and, as security advice says, generates a new key. The new chain is perfectly valid, and the app that pinned the old leaf key refuses it. Every installed copy fails at the same moment. If 85% of users update within a week, 1,500 of
10,000 are still locked out after seven days, and the bank's support line and reputation pay for it.

### 4. Pins chosen so that rotation does not break the app

Pinning the issuing CA's key keeps working through renewals and key changes of the leaf, and the interceptor still fails, because its chain does not contain that key. Pinning the leaf together with a backup pin (a key made in advance and kept offline) lets the server switch to the backup key with no app update.
In report-only mode, a mismatch is recorded and the connection is accepted: a way to learn what a pin policy would have broken before it enforces anything.

### 5. Why browsers stopped letting servers set pins

With HPKP the server's response told the browser which keys to pin and for how long. A meddler who answers just one request with a header naming the meddler's keys and a max-age of a year makes returning visitors refuse the genuine server for up to a year: the genuine site is blocked on day 30
and accepted again only after the pin expires. A site that pins its own key and then loses it (and its backup) locks itself out in the same way. The risk to sites was larger than the protection, and browsers removed the feature in favour of Certificate Transparency.

## The verdict

Use pinning in apps where the threat is real and you control updates: pin the issuing CA's key or the leaf with a backup key, keep a report-only mode and a tested rotation, and have a way to update users. Never accept server-set pins for a website; use Certificate Transparency, CAA and short-lived certificates.

## How to recognise this in code you did not write

- `CertificatePinner.Builder().add(host, "sha256/...")` in OkHttp.
- `<pin-set expiration="..."><pin digest="SHA-256">` in Android network security configuration.
- `Public-Key-Pins` headers in old captures.
- "SSL pinning bypass" in a mobile security report.

## Where you have already met this

- `CertificatePinner` in OkHttp and the `<pin-set>` in Android's network security configuration.
- `NSPinnedDomains` in iOS.
- "SSL pinning bypass" in mobile penetration-testing guides.
- The 2017–2018 removal of HPKP from Chrome and Firefox.
