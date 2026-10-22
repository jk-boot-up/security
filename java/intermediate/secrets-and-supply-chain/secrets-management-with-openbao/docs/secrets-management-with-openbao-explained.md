# Secrets Management with OpenBao, Explained

## The idea in one sentence

A secrets server turns a secret that never changes into a login that lasts minutes, belongs to one application and can be taken back.

## The 5 acts

### 1. A secrets server, and who may ask

The store's payment key lives in OpenBao, not in the code, the environment or the image. shop-api logs in with its role id and its secret id and gets a token with the policies default and shop-api for 120 seconds. The same secret id again is refused, and so is one nobody issued. shop-api reads its payment key but is refused the reports key; reports reads its own key and is refused the payment key. A request with no token is refused. A policy names exactly what each application may read, and everything else is refused.

### 2. Secrets that stay still: versions, rotation and the audit trail

The store reads the payment key, version 1. The provider issues version 2 and it is written as version 2; during the changeover both are readable by number, and the current version is now version 2. The application cannot destroy versions: refused. After every server has switched, the operator destroys version 1; reading version 1 is now refused with 404, and version 2 still works. The audit trail recorded the requests, the path and the application that asked, and not the key itself: values are written as HMACs.

### 3. Database logins that are leased

shop-api asks and gets a database login of its own, named v-approle-shop-api-<random>, for 8 seconds. Reports gets a different one; no two applications share one. Both connect to the real database. shop-api's login reads orders and cannot read the card numbers. shop-api asking for reports' login is refused. The shop renews its lease asking for 600 seconds and is capped at the maximum of 20. Reports never renewed: 9 seconds on, its lease has ended and the login is removed. The shop's renewed login still connects. The operator revokes the shop's lease, and the login no longer connects.

### 4. Rotation, and the blast radius of a leak

The payments service has a static login that OpenBao manages. A thief copies its password, which connects. OpenBao rotates it: the new password differs, the new one connects, and the thief's copy does not. Three logins leased to shop-api's servers all connect; a thief copies one. The leak is noticed and the operator revokes every lease under database/creds/shop-api at once: 0 logins still connect. The other applications are untouched: reports still gets a login.

### 5. The bill

The operator seals OpenBao, as in an incident. The application asks for its payment key and is refused with 503, and for a new login, refused with 503: the store cannot start a server while the secrets service is down. A login it already holds keeps working until its lease would have ended. The operator unseals with the key and the application reads again. The secrets service holds every secret, so it needs its own access control, audit and backups. The application still needs a first credential, its role id and a secret id; platform identity, in the advanced track, removes it. Leases and rotation need applications that can reconnect with a new login without a restart.

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
