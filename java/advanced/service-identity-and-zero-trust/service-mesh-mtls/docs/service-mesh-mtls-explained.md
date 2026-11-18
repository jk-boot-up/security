# Service Mesh and Automatic mTLS, Explained

## The idea in one sentence

The mesh names the caller and hides the call; a policy decides, and someone still owns the ServiceAccounts.

## The 6 acts

### 1. Before and after the proxies

The input: web calls GET http://payments.bank:8080/echo?msg=ACCT-4417-transfer-9000 while tcpdump runs on the node.
Before the proxies (0 of 5 pods have one): HTTP 200, the capture holds the request line GET /echo?msg=ACCT-4417-transfer-9000, the account is readable (yes).
After injection (4 of 5 pods; legacy/batch left out): HTTP 200, no request line on the wire (none), the account is not readable (no), and packets went to port 4143, the proxy's inbound port.
The result: payments' proxy counted the request under the client certificate web.bank.serviceaccount.identity.linkerd.cluster.local, and web's proxy under the server certificate payments.bank...: both ends proved who they are.

### 2. An identity from each ServiceAccount

The input: the pod spec of payments: namespace bank, serviceAccountName payments.
How the name is made: payments (the ServiceAccount) . bank (the namespace) . serviceaccount.identity (fixed) . linkerd (Linkerd's namespace) . cluster.local (the trust domain).
The result: linkerd identity prints the subject of each proxy's certificate; web, payments, ledger and statements each hold the name the rule gives, 4 of 4. The identity service signs a certificate only after checking the pod's ServiceAccount token.

### 3. Mutual TLS alone does not decide

The input: every caller (web, payments, ledger, statements and legacy/batch) calls every bank service: 5 x 4 - 4 = 16 pairs. The design allows 2: web -> payments and payments -> ledger.
The result: 16 of 16 answered, 14 not in the design, 0 missing. legacy/batch, with no proxy, reached 4 of 4. Ledger's proxy let in a connection with no identity (yes), and the node itself, with no proxy and no identity, gets HTTP 200. Linkerd's default inbound policy, all-unauthenticated, lets every caller in: mutual TLS encrypts and names, but decides nothing.

### 4. A policy names who may call

The input: Policies.forService writes a Server (port 8080 of the pod) for web, statements, payments and ledger, and for payments and ledger a MeshTLSAuthentication naming one identity and an AuthorizationPolicy that requires it. A Server alone refuses every caller.
The result: 2 of 16 answered, 0 not in the design, 0 missing; the measurement equals the design (yes). Calls to ledger one by one: statements HTTP 403, web 403, legacy/batch 403, payments 200. Ledger's proxy counted the refusals by the caller's identity: no identity 2, statements.bank 2, web.bank 2 (once in the sixteen calls, once in the call one by one).

### 5. What the mesh does not protect

Root on the node: the node's own call to ledger gets HTTP 403, but a capture entered into the payments pod's network namespace reads the request between the proxy and the app (yes).
A pod outside the mesh: web calls legacy/batch, HTTP 200, and the account number is readable on the wire (yes).
An allowed caller: web sends ' OR '1'='1 to payments, HTTP 200, and the string arrives intact (yes): the mesh judges the caller, never the request.
Who may take an identity: a new pod in bank that runs as the payments ServiceAccount calls ledger: HTTP 200. A ServiceAccount also called payments in namespace shadow gets HTTP 403, its identity is payments.shadow, not payments.bank. The web ServiceAccount may not create pods in bank (no); a developer with the namespace's edit role may (yes).

### 6. Certificate lifetime and renewal

The input: linkerd install --identity-issuance-lifetime=5m0s; Linkerd's own default is 24h0m0s, read from the CLI's help.
How the lifetime is read: the certificate states Not Before and Not After. Linkerd backdates the start and extends the end by 20 seconds each, so a clock a little wrong still works: 340 s - 2 x 20 s = 300 s = 5 minutes.
The trust anchor that signs the issuer is valid for 365 days.
The result after the renewal (checked every 15 s): a newer certificate (yes), the same name (yes), the pod restarted (no), payments calls ledger HTTP 200. A certificate is trusted until it expires: its lifetime is how long a stolen one is useful.

## The verdict

Use a mesh when many services need identities and encryption without code changes, write a policy for every service, keep pods and ServiceAccount creation under control, and treat the node and the application as outside what the mesh protects.

## How to recognise this in code you did not write

- A mesh installed and called zero trust with no policy.
- A namespace where anyone can create pods.
- Some services left out of the mesh without a note.
- An alert for expired certificates that nobody owns.

## Where you have already met this

- A security review that asks whether traffic inside the cluster is encrypted.
- A network policy that allows a whole namespace.
- A shared database password between services.
- A certificate that expired on a Sunday.
