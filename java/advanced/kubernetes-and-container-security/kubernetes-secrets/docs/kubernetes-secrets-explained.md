# Kubernetes Secrets, Explained

## The idea in one sentence

A Secret is only as safe as the people and the disks that can reach it.

## The 5 acts

### 1. Base64 is not encryption

The card key stored in a Secret comes back as base64 and decodes at once. The auditor ServiceAccount may only list secrets and reads the value (yes); a ServiceAccount with no rule on secrets cannot (no).

### 2. Raw etcd, before

etcdctl inside the etcd pod reads /registry/secrets/payments/card-key. The value contains the card key as plain text (yes), not even as base64 (no), and not every secret in etcd is encrypted (no).

### 3. Encryption at rest

The API server restarts with an EncryptionConfiguration. card-key stays plain text, card-key-2 is stored as k8s:enc:aescbc with no plain text. After every secret is rewritten, every secret in etcd is encrypted and the card key is nowhere in etcd. The key is a file on the node, and the auditor still reads the value.

### 4. OpenBao and a 10 second token

OpenBao holds the database password: no Secret and no etcd value contains it. payments-app logs in with its ServiceAccount token (HTTP 200), gets a token of 10 seconds and reads the password (200). other-app is refused (403), and the same token is refused 12 seconds later (403).

### 5. A bound ServiceAccount token

kubectl create token gives a token with a lifetime of 600 seconds, the audience openbao and the pod report-job. The old token Secret has no expiry and no pod. When the pod is deleted the API server stops accepting the bound token (no) and still accepts the old one (yes).

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
