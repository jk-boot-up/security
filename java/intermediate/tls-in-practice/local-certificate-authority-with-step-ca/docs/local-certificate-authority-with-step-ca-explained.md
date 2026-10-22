# A Local Certificate Authority with step-ca, Explained

## The idea in one sentence

A local certificate authority is a service that decides which names get which certificates, for how long, and for whom, and clients trust it because they were given its root.

## The 5 acts

### 1. A certificate authority that runs

step ca init creates a self-signed root and an issuing authority signed by the root that may not create authorities below it. step-ca serves https with a certificate from that authority. A client that trusts the root reads its health; a client that has not been given the root refuses. The root's private key is a file on this machine, encrypted with the CA's password.

### 2. A certificate for a service, with a token

The web team's service asks with a one-time token for www.shop.localhost and gets a certificate that lasts 24 hours. A client that trusts the root accepts it. The same token used a second time is refused. A token for another name, used for www.shop.localhost, is refused. Asking for 2000 hours is refused, and so is the name pay.example even with a valid token, because the policy allows only localhost and names under shop.localhost.

### 3. A certificate for a service, with ACME

The service asks through the ACME provisioner and answers the CA's challenge on port 80 of its own name. The certificate is issued for 24 hours and a client that trusts the root accepts it. ACME for a name outside the policy is refused. ACME proves that the asker controls the name now; it does not prove that the asker is the team that should have it, which is what the policy and the tokens are for.

### 4. Renewing before it ends

The service renews with its certificate and its key: a new serial number, the same key, and a later end. Renewing with a new key gives a different key. A service that holds another certificate's key cannot renew this one, because the renewal is a TLS call authenticated by the certificate's own key. With a day of life, a renewal job that stops is noticed within a day, when the certificate runs out.

### 5. Revocation, and the bill

The acme certificate's key is reported stolen and revoked. The CA then refuses to renew it and publishes a revocation list signed by the issuing authority with its serial number. A client that checks the list refuses the certificate. A client that never checks still accepts it. A client that wants to check and has no list refuses: it fails closed. The other certificate is accepted against the same list. The issuing key is online and the root key is on disk, so the machine and the password are what the trust rests on.

## The verdict

Keep certificates short so that renewal replaces revocation, restrict names with a policy and a name constraint, protect the issuing key like the crown jewel, give clients the root from a safe place, and make every service renew on its own.

## How to recognise this in code you did not write

- `step ca certificate`, `step ca renew` or `step ca revoke`.
- `acme` as a provisioner in `ca.json`.
- A `root_ca.crt` in a trust store.
- A systemd timer or a daemon that renews certificates.

## Where you have already met this

- `step ca certificate` or `step ca renew` in a script.
- `acme` as a provisioner name in a CA's configuration.
- A `root_ca.crt` copied into a trust store.
- A job that restarts a service after it renews a certificate.
