# Certificates and Chain of Trust with Bouncy Castle and OpenSSL, Explained

## The idea in one sentence

A certificate authority is a set of builders and rules: certificates carry extensions that fix what each key may do and which names it may vouch for, revocation lists withdraw them, signing requests let the private key stay home, and the whole thing is only as safe as the authority's key.

## The 5 acts

### 1. A chain built with Bouncy Castle

Before building, each builder is asked for something: the root is a CA with 1 more CA allowed below it and signs itself; the intermediate is a CA with 0 below and may only certify names under shop.example; the server certificate is not a CA, for shop.example, to sign data.
After building, the certificates are read back: CN=Example Root CA with 1 CA below, CN=Example Issuing CA with 0 and the name rule shop.example, and CN=shop.example, which is not a CA.
Key usage is a row of bits: bit 0 signs data, bit 5 signs certificates, bit 6 signs revocation lists. The intermediate's bits 0 to 8 are 000001100, certificates and lists; the server's are 100000000, data only. Each certificate names its parent's key identifier, so a client can find the right parent.

### 2. A limit the authority carries in its own certificate

Before the check, the intermediate carries a name constraint in its own certificate: shop.example. It signs certificates for shop.example, api.shop.example and other.example.
Step, the rule by hand: a name passes if it is shop.example itself, or ends with .shop.example. shop.example is the same name, api.shop.example ends with .shop.example, other.example is neither, and evilshop.example is neither too: the dot matters.
After the check, the JDK's PKIX validator agrees: shop.example and api.shop.example are accepted, other.example is refused with INVALID_NAME. So a stolen intermediate key can sign only names under shop.example.

### 3. A revocation list signed by the issuer

Before revocation, the intermediate publishes a signed list with 0 entries, and the chain is accepted. After revocation, because the shop's key is lost, the list has 1 entry: serial 3, KEY_COMPROMISE. Its signature checks with the intermediate's key and not with the root's, and the chain is refused: REVOKED.
Without a good list, the client cannot find out: a list signed by a stranger, or no list at all, gives false, UNDETERMINED_REVOCATION_STATUS. This client refuses when it cannot find out; many browsers go ahead instead, which is why short-lived certificates matter.

### 4. Asking for a certificate without handing over the private key

The request: the shop makes its own key pair, keeps the private key at home, and signs a request naming [shop.example] with its EC public key. The signature proves the sender holds the private key; with one letter of the name changed on the way, it no longer does.
The response: the authority's policy allows names under shop.example, so it signs a certificate with serial 10 that holds the shop's own public key, and refuses bank.example, which is outside shop.example. As PEM, the certificate starts with -----BEGIN CERTIFICATE----- and reads back as the same certificate.

### 5. A stolen authority key, and the bill

Before revocation, a thief has the intermediate's private key and signs shop.example, serial 77: the forged chain is accepted, and everything below the stolen key falls. After revocation, the root's list names serial 2, CA_COMPROMISE: the forged chain and the shop's real chain are both refused, so the shop needs a new certificate from a new intermediate.
The bill: revocation protects only clients with the new list; builders are short to write and easy to get subtly wrong, as a missing critical flag or constraint weakens a rule; a trusted root may vouch for any name, so keep the store small, and keep CA keys offline or in hardware.

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
