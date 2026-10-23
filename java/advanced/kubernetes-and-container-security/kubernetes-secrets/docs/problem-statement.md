# Problem Statement

## The scenario

A bank stores a card key and a database password in Kubernetes and believes they are protected.

## The naive version

Create a Secret and assume it is encrypted.

## What this project must deliver

- A real cluster and a Secret that decodes with no key.
- The raw etcd value before and after encryption at rest.
- OpenBao with a 10 second token.
- A bound ServiceAccount token against the old one.
