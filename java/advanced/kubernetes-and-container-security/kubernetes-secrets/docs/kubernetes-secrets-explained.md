# Kubernetes Secrets, Explained

## The idea in one sentence

A Secret is only as safe as the people and the disks that can reach it.

## The 5 acts

### 1. Base64 is not encryption

The input is a Secret payments/card-key with the key demo-card-vault-key-001. The API returns data.key = ZGVtby1jYXJkLXZhdWx0LWtleS0wMDE=.
How base64 is undone, on the first four characters: Z G V t are places 25, 6, 21 and 45 in the alphabet; as 6 bits each they are 011001 000110 010101 101101; the same 24 bits cut into 8 are 01100100 01100101 01101101, the bytes 100 101 109, "dem". No key is used anywhere.
The result: the auditor ServiceAccount, whose Role allows only list on secrets, reads the value (yes), because a list returns every item with its data; the ServiceAccount nobody, with no rule, is refused (no).

### 2. Raw etcd, before encryption

The input: etcdctl, run inside the etcd pod, reads the key /registry/secrets/payments/card-key, the value exactly as the API server wrote it.
The result: the value starts with k8s, a zero byte and the protobuf form of the object (v1, Secret). The card key is in it as plain text (yes); not even its base64 form is there (no), because protobuf stores bytes.
The 7 bytes in front of the card key, in hex: 0a 03 is field 1, length 3; 6b 65 79 is "key", the name in data; 12 17 is field 2, length 0x17 = 23; then the 23 characters of the card key. Of the 2 secrets in etcd, 0 are encrypted.

### 3. Encryption at rest

The input: an EncryptionConfiguration for secrets with two providers: aescbc with key1 (32 random bytes, never printed) first, then identity, which still reads values stored plain. The API server is restarted to read it.
The result before the rewrite: card-key, written before, is still plain text (yes); card-key-2, written after, is encrypted (yes) with no plain text (no); kubectl returns it as before (yes); 1 of 3 secrets is encrypted.
How the encrypted value reads: it starts with k8s:enc:aescbc:v1:key1: — k8s:enc (encrypted by the API server), aescbc (the provider, AES in CBC mode), v1 (its format), key1 (the key's name) — then bytes that look random, with no "Secret" in them.
After kubectl replace of every secret, card-key is encrypted too, 3 of 3 are encrypted, and the card key is nowhere in etcd. What it does not change: the key is a file on the control-plane node (yes), and the auditor still reads the value (yes).

### 4. OpenBao and a 10 second token

The input: an OpenBao role auth/kubernetes/role/payments for the ServiceAccount payments/payments-app, audience openbao, policy payments-read (read secret/data/payments/db), tokens of 10 seconds. The database password is in OpenBao, in no Kubernetes Secret and not in etcd.
The request: payments-app sends the token the kubelet put in its pod: sub system:serviceaccount:payments:payments-app, aud openbao. OpenBao asks the API server who it belongs to (a TokenReview).
The response: HTTP 200, an OpenBao token for 10 seconds with the policies default and payments-read. Read at once: HTTP 200 and the right value. Read again 12 seconds later: 12 > 10, the token has expired, HTTP 403. other-app logs in with its own token (sub ...:other-app) and gets HTTP 403: it is not the role's ServiceAccount.

### 5. A bound ServiceAccount token

The input: kubectl create token payments-app for 10 minutes, audience openbao, bound to the pod report-job; and a Secret of type service-account-token, the old kind, that Kubernetes fills with a token.
How the lifetime is worked out: a JWT is signed, not encrypted, so its claims are read with no key. The bound token has the claims aud exp iat iss jti kubernetes.io nbf sub; exp - iat = 600 seconds; audience openbao; pod report-job. The old token has 6 claims, with no exp and no aud: it never expires and is tied to no pod.
The result, asked of the API server with a TokenReview: while report-job runs, both are accepted; after the pod is deleted, the bound token is refused (no) and the old one is still accepted (yes).

## The verdict

Turn on encryption at rest with a KMS provider and rewrite old secrets, give list and get on secrets to very few, keep long-lived secrets in an external store, and use bound tokens that expire and name an audience.

## How to recognise this in code you did not write

- A Secret committed to Git.
- A role with list on secrets everywhere.
- An etcd backup left on a shared disk.
- A token Secret created years ago.

## Where you have already met this

- A Secret committed to Git as base64.
- A role that allows list on secrets in every namespace.
- An etcd backup in a shared bucket.
- A service account token that never expires.
