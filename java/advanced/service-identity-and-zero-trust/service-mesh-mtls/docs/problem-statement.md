# Problem Statement

## The scenario

A bank wants its services to prove who they are and to stop talking in clear inside the cluster, without changing every service.

## The naive version

Trust the cluster network and let every pod call every pod.

## What this project must deliver

- A mesh that issues identities from ServiceAccounts.
- Automatic mutual TLS, measured on the wire.
- A policy that names who may call.
- A list of what the mesh does not cover.
