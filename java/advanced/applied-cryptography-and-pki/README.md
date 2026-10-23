# Applied cryptography and pki

Certificates over time, TLS attacks, post-quantum cryptography and hardware keys

## The projects

| # | Project | What it shows |
| --- | --- | --- |
| 1 | [certificate-lifecycle](certificate-lifecycle) | Issue certificates from a private root and intermediate, revoke them with a CRL, an OCSP responder and a stapled response, replace them with short-lived certificates that step-ca renews, and watch what a missed renewal does |
| 2 | [tls-attacks-and-downgrades](tls-attacks-and-downgrades) | Run deliberately weak local TLS servers, scan them with openssl and testssl.sh against the weaknesses that were planted, watch a client fall back to TLS 1.0, read a recorded session with a leaked key, and check what TLS 1.3 removed |
| 3 | [post-quantum-cryptography](post-quantum-cryptography) | Use ML-KEM and ML-DSA from the JDK, combine X25519 with ML-KEM in a hybrid exchange, measure what the new sizes cost, and count where a bank's code still depends on RSA and elliptic curves |
| 4 | [crypto-agility](crypto-agility) | Inventory the algorithms in a bank's code, store data in a versioned envelope, and rotate the algorithm and the key under 10,100 records without losing one |
| 5 | [hardware-backed-keys](hardware-backed-keys) | Generate a bank's payment-signing key on a PKCS#11 token, sign with it from Java, try 5 ways to get the key out, and measure what the token does not protect against |

The module plan is in [`../../../docs/advanced-plan.md`](../../../docs/advanced-plan.md). Run a project as in the other tracks: `./gradlew test`, then `./gradlew run`.
