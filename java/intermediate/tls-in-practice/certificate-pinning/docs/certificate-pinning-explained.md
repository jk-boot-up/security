# Certificate Pinning and What It Costs, Explained

## The idea in one sentence

A pin narrows trust from every authority on the device to a key you chose, so it stops interception by a trusted-but-unexpected root, and costs a planned rotation, backup pins and an update path.

## The 5 acts

### 1. An app that trusts the device

The phone trusts Public Root CA, shipped with it, and Helpful Proxy Root, added by a company proxy, a tool or malware. The genuine chain is api.bank.example signed by Public Issuing CA; the interception chain is api.bank.example signed by Helpful Proxy CA.
ChainWalk does by hand what validation does: the genuine chain's top is signed by Public Root CA, and the interception chain's top is signed by Helpful Proxy Root. Both roots are on the phone, so both checks pass.
The app with no pins (PinnedClient, which runs the JDK's PKIX validator) accepts both. The proxy now reads every request.

### 2. An app that pins the server's key

The input is the leaf's public key, an EC P-256 key, as SubjectPublicKeyInfo: 91 bytes, a 26-byte header naming the algorithm and the curve and the 65-byte point. Its SHA-256 hash is 32 bytes, and the hash in Base64 is the 44-character pin.
PinSteps does the Base64 by hand: the first three bytes of the hash, 2f 3a bf, are 24 bits, cut into four 6-bit values (11, 51, 42, 63), which are the letters L, z, q and /. 32 bytes are 10 groups of 3 and 2 left over, so 11 groups of 4 letters: 44 characters, the last one '='. The result is the same as the JDK's Base64.
By hand, the genuine chain contains the pinned leaf key, and the interception chain contains none of its two keys. So the app accepts the genuine server, refuses the interception by pin, and on a phone without the proxy's root, validation refuses it first.

### 3. The server renews with a new key

On day 80 the bank renews its certificate and makes a new key, as many renewals do. The app's pin and the new key's pin differ, so by hand none of the renewed chain's two keys is pinned, and the app refuses the bank's own server with REFUSED_BY_PIN.
The fleet worked out: 10000 installs all fail on day 0. If 85% update within a week, 10000 - 10000 x 85 / 100 = 1500 are still locked out after a week.

### 4. Pins chosen so that rotation does not break the app

Three apps ship different pins. The issuer pin is the Public Issuing CA's key, which a leaf renewal does not change. Leaf + backup pins the leaf key and a spare key that was generated in advance and kept offline. Report-only pins the leaf key but only reports a mismatch.
By hand: the renewed chain contains the issuer's key; the interception chain contains none; when the server switches to the backup key, the chain contains the backup key; the renewed chain contains no key of the report-only app.
The results: ACCEPTED, REFUSED_BY_PIN, ACCEPTED, and ACCEPTED_REPORT_ONLY with one report collected, which tells the team what would have broken.

### 5. Why browsers stopped letting servers set pins

Browsers once let a server set pins with a header (HPKP). A meddler who answers one first visit sends Public-Key-Pins with the pins of its own keys and max-age=31536000. Worked out, 31536000 s / 86400 s a day = 365 days.
By hand: on day 30, 30 < 365, so the meddler's pins still apply; on day 400, 400 > 365, so they have expired. PinStore agrees: the genuine bank server is blocked for every returning visitor on day 30, and accepted again on day 400.
The same trap catches the site itself: it pins its own leaf and backup keys, then renews with a new key that is neither, and is blocked until day 365, with no way to undo it.

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
