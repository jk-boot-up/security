# Problem Statement

## The scenario

A web shop runs in containers. The application has flaws, as applications do, and the team wants to limit what one compromised container can do.

## The naive version

Run it with whatever flags make it start, and fix problems as they appear.

## What this project must deliver

- A container specification covering user, capabilities, privilege, file system, seccomp, host access, limits, secrets and image.
- An attacker model with thirteen attacks, each tied to the setting that decides it.
- Pod Security Standards levels with the reasons for failing the next level.
- Renderers for Docker flags and a Kubernetes securityContext.
