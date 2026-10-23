# Session Guide — Kubernetes Secrets

## Learning Objectives

By the end of the session you can:

- explain why base64 is not encryption
- read a Secret raw from etcd and tell whether it is encrypted
- turn on encryption at rest and rewrite existing secrets
- explain what a short-lived and a bound token change

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: Base64 is not encryption | 7 min |
| 0:17 | Act 2: Raw etcd, before | 7 min |
| 0:24 | Act 3: Encryption at rest | 7 min |
| 0:31 | Act 4: OpenBao and a 10 second token | 7 min |
| 0:38 | Act 5: A bound ServiceAccount token | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Run the demo and compare the etcd lines of the second and third act. In the fourth act find the two 403 lines and what each one means.

## Exercises

1. Remove identity from the providers and find what breaks for an old secret.
2. Add a second key, make it first, and rewrite the secrets.
3. Change the token TTL to 60 seconds and check when the read fails.
4. Write an admission rule from kubernetes-admission-control that refuses a Secret of type service-account-token.
