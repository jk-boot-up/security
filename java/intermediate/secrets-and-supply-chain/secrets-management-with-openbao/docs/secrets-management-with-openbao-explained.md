# Secrets Management with OpenBao, Explained

## The idea in one sentence

A secrets server turns a secret that never changes into a login that lasts minutes, belongs to one application and can be taken back.

## The 5 acts

### 1. A secrets server, and who may ask

The input is the policy the operator wrote for shop-api: path "secret/data/shop-api/*" and path "database/creds/shop-api", both with capabilities ["read"]; reports has the same with its own name.
The request is POST auth/approle/login with shop-api's role id and a fresh secret id that works once. The response: allowed (200), a token with the policies [default, shop-api], for 120 seconds.
The same secret id again, as a thief who saw it would use it, is refused (400), and so is a secret id nobody issued.
How the policy decides a read: a path ending in * covers every path below it, any other path covers only itself, and anything no rule covers is refused. shop-api may read secret/data/shop-api/payment (200) but not secret/data/reports/warehouse or database/creds/reports (403); reports the other way round; a request with no token is refused (403). The teaching class PolicyRule gives the same answer as OpenBao in all six cases.

### 2. Secrets that stay still: versions, rotation and the audit trail

Before the rotation the store reads the current payment key, {api-key=key-v1}. The provider issues v2, and the operator writes it as version 2. During the changeover both are readable by number: version 1 {api-key=key-v1}, version 2 {api-key=key-v2}, and the current version is now version 2.
Destroying version 1 once every server has switched: by the application, refused (403); by the operator, allowed (204); then version 1 is refused (404) and version 2 is allowed (200).
The audit trail recorded the requests for secret/data/shop-api/payment and the application that asked, but not the key itself. How it hides a value: OpenBao writes it as an HMAC-SHA256 with the server's own key. key-v2 becomes hmac-sha256: followed by 64 hex characters, and sys/audit-hash gives exactly the hash that is in the log.

### 3. Database logins that are leased

The input is the role the operator set up: for every request OpenBao runs CREATE ROLE "{{name}}" WITH LOGIN PASSWORD '{{password}}' VALID UNTIL '{{expiration}}' and GRANT SELECT ON orders TO "{{name}}", with a time to live of 8 s and a maximum of 20 s.
The requests GET database/creds/shop-api and database/creds/reports return two logins, v-approle-shop-api-... and v-approle-reports-..., different, each with a lease of 8 s. Both connect to the real PostgreSQL; shop-api's login reads orders and not card_numbers.
The shop renews and asks for 600 seconds: allowed (200), but granted min(600, 20 - its age), so at most 20.
After 9.5 seconds, past the 8 s time to live, the reports login, never renewed, no longer connects; the renewed shop login still does. The operator revokes the shop's lease: allowed (204), and the login no longer connects.

### 4. Rotation, and the blast radius of a leak

Before the rotation, the static login payments, which OpenBao manages: a thief copies its password, and it connects.
After the rotation (POST database/rotate-role/payments): the new password differs, the new password connects, and the thief's copy does not.
Before the revocation, three leased logins of shop-api all connect, and a thief copies one. After revoking every lease under database/creds/shop-api: allowed (204), 0 of 3 logins still connect, and reports still gets a login: allowed (200).

### 5. The bill

The operator seals OpenBao, as in an incident: allowed (204). While it is sealed, the payment key is refused (503) and a new login is refused (503), so no new server of the store can start; a login the application already has still connects.
After unsealing with the key: allowed (200), and the payment key is allowed (200) again.
What it costs: OpenBao holds every secret, so it must be guarded, audited and backed up; this dev server keeps everything in memory, a real one stores it; the first credential, the role id and secret id, remains; applications must take a new login without a restart.

## The verdict

Keep secrets in a service, give each application its own role and policy, lease database logins and renew them, rotate static passwords, audit every request, revoke on a leak, and plan for the day the service is sealed.

## How to recognise this in code you did not write

- A user name that starts with `v-` in a database.
- `role_id` and `secret_id` in a deployment.
- `lease_duration` and `renewable` in an API answer.
- An unseal key held by several people.

## Where you have already met this

- `vault read database/creds/...` and `v-approle-...` user names in a database.
- `role_id` and `secret_id` in a deployment pipeline.
- A `403 permission denied` from a secrets server.
- A secrets service sealed after a restart.
