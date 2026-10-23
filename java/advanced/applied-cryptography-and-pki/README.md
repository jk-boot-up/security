# Applied cryptography and pki



## The projects

| # | Project | What it shows |
| --- | --- | --- |
| 1 | [tls-attacks-and-downgrades](tls-attacks-and-downgrades) | Run deliberately weak local TLS servers, scan them with openssl and testssl.sh against the weaknesses that were planted, watch a client fall back to TLS 1.0, read a recorded session with a leaked key, and check what TLS 1.3 removed |
| 2 | [post-quantum-cryptography](post-quantum-cryptography) | Use ML-KEM and ML-DSA from the JDK, combine X25519 with ML-KEM in a hybrid exchange, measure what the new sizes cost, and count where a bank's code still depends on RSA and elliptic curves |
| 3 | [crypto-agility](crypto-agility) | Inventory the algorithms in a bank's code, store data in a versioned envelope, and rotate the algorithm and the key under 10,100 records without losing one |

The module plan is in [`../../../docs/advanced-plan.md`](../../../docs/advanced-plan.md). Run a project as in the other tracks: `./gradlew test`, then `./gradlew run`.
