# Secrets and supply chain

Two ways an attacker can win without touching the application's code: by finding a secret it depends on, or by changing what it is built
from. This module covers OWASP Top 10 (2025) A03 Software Supply Chain Failures and A08 Software or Data Integrity Failures, with SLSA
and NIST SSDF practices.

## The projects

| # | Project | What it shows | Maps to |
| --- | --- | --- | --- |
| 1 | [secrets-management](secrets-management) | Secrets found in files, environment variables and image layers; run-time injection from an owner-only file, redaction, leased per-app logins, rotation and the blast radius of a leak | A02, A04, CWE-798, CWE-522 |
| 2 | [artifact-signing](artifact-signing) | Signed release statements over exact digests, keys pinned per channel with rotation and revocation, and a Merkle transparency log with inclusion and consistency proofs | A08, A03, CWE-494, SLSA |
| 3 | [sbom-and-provenance](sbom-and-provenance) | Know what is inside a release, and prove where it came from | A03, A08 |
| 4 | [dependency-confusion](dependency-confusion) | If a build can find your private name in a public place, someone else will put a package there | A03, CWE-427, CWE-829 |
| 5 | [secrets-management-with-openbao](secrets-management-with-openbao) | A real secrets server hands each application its own short-lived database login, and takes it back when the lease ends or a leak is noticed | A02, CWE-798 |
| 6 | [artifact-signing-with-cosign](artifact-signing-with-cosign) | A tag is a name, a digest is a checksum, and a signature on the digest is the only thing a deployment gate can rely on | A08, SLSA |

A project with `-with-` in its name is the same lesson with a framework or a local tool, and is its own self-contained project.

## How the module is judged

- **Break it, then fix it,** or build the control and prove each refusal with a test.
- **Every claim is a number** that a test checks against a golden output file.
- **A threat model for every project,** with a diagram, in `docs/threat-model.md`.

See the [intermediate plan](../../../docs/intermediate-plan.md) for the projects still to come.
