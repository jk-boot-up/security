# Certificates and Chain of Trust with Bouncy Castle and OpenSSL, Explained

## The idea in one sentence

A certificate authority is a set of builders and rules: certificates carry extensions that fix what each key may do and which names it may vouch for, revocation lists withdraw them, signing requests let the private key stay home, and the whole thing is only as safe as the authority's key.

## The 5 acts

### 1. A chain built with Bouncy Castle

The root allows one authority below it, the intermediate allows none, and the server certificate may not issue certificates at all. An authority's key may sign certificates and lists but not data, and the server's key may sign data but not certificates. Each certificate names
its parent's key identifier, which is how a client finds the right parent. The server certificate names shop.example. All of it is written with builders, and no byte of the encoding is written by hand.

### 2. A limit the authority carries in its own certificate

The intermediate carries a name constraint: it may only certify shop.example and names below it. A certificate for shop.example is accepted. A certificate for api.shop.example from the same intermediate is accepted too. A certificate for other.example from the same intermediate
is refused with the reason INVALID_NAME. The client checks the limit, so a stolen intermediate key can only be misused for the names it was limited to.

### 3. A revocation list signed by the issuer

The issuer publishes a signed list. An empty list means nothing is revoked and the chain is accepted. When the shop's key is lost, the intermediate lists its serial number, and the chain is refused with the reason REVOKED. The list's signature verifies with the intermediate's key
and not with the root's. A list signed by someone else is not used, and with no list at all the status is undetermined and the chain is refused. This client refuses when it cannot find out. Many browsers go ahead instead, which is why short-lived certificates matter.

### 4. Asking for a certificate without handing over the private key

The shop makes its own key pair and signs a request naming shop.example; the private key stays with the shop. The request's signature proves the sender holds the private key. Change one letter of the name on the way, and the signature no longer verifies.
The authority's policy allows names under shop.example, so it signs, and the certificate holds the shop's public key. A request for bank.example is refused. As PEM text the certificate starts with a BEGIN CERTIFICATE line and reads back as the same certificate.

### 5. A stolen authority key, and the bill

If someone steals the intermediate's private key and signs their own certificate for shop.example, the forged chain is accepted: a stolen authority key breaks everything below it. When the root lists the intermediate as compromised, the forged chain is refused, and so is the shop's real chain.
The shop needs a new certificate from a new intermediate, and revocation only protects clients that fetched the new list. The bill: builders make certificates short to write and easy to get subtly wrong, and trusting a root is a decision, so keep the store small and keep authority keys offline or in hardware.

## The verdict

Use a public authority for public sites and your own small authority, with name constraints, short lifetimes and an offline root, for internal services. Check what you build with a second tool, and treat every extension as a decision.

## How to recognise this in code you did not write

- `openssl x509 -noout -text` output in troubleshooting guides.
- Name-constrained intermediates in enterprise and service mesh designs.
- CRL and OCSP settings in web server and client configuration.
- Certificate signing requests created by `openssl req` or `keytool -certreq`.

## Where you have already met this

- `openssl x509 -text`, which prints exactly the extensions this project writes.
- Let's Encrypt and other public authorities, which publish intermediates and revocation information.
- Internal certificate authorities in service meshes and Kubernetes.
- `keytool -importcert`, which adds a root to a trust store.
