# Service Mesh and Automatic mTLS, Explained

## The idea in one sentence

The mesh names the caller and hides the call; a policy decides, and someone still owns the ServiceAccounts.

## The 5 acts

### 1. Before and after the proxies

A packet capture beside the node sees the call from web to payments. Before injection the account number is in the capture. After, the capture holds only traffic to the proxy port, and the proxy counters show web as the caller seen by payments and payments as the server seen by web.

### 2. Identities from ServiceAccounts

The identity service signs each proxy's certificate after checking the pod's ServiceAccount token. The name is the ServiceAccount, the namespace and the trust domain, so a ServiceAccount of the same name in another namespace is another identity.

### 3. Mutual TLS is not a decision

Linkerd's default inbound policy lets every caller in. Mutual TLS encrypts and identifies, but nothing says who may call. The batch job with no proxy reaches all 4 services and the node gets HTTP 200.

### 4. A policy names who may call

Each service gets a Server for its port and an AuthorizationPolicy naming the identity allowed. Statements, web calling the ledger directly, the pod with no proxy and the node are refused with 403; the ledger's proxy counts the refusals by caller.

### 5. What the mesh does not protect

The injection string passes through intact. A capture inside the payments pod shows the account number between the proxy and the app. A call to a pod with no proxy is readable. A pod that runs as the payments ServiceAccount gets payments' access. A certificate lives until it expires; here it was issued for 5 minutes, renewed with the same name and no restart.

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
