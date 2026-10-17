# Safe habits

What keeps a system safe over time: giving only the access that is needed, keeping software current, and being able to recover.

## The projects

| # | Project | What it shows |
| --- | --- | --- |
| 1 | [least-privilege](least-privilege) | Roles, deny by default, blast radius, separation of duties, expiring grants and unused permissions |
| 2 | [least-privilege-with-spring-security](least-privilege-with-spring-security) | Method security, an authorization manager and a stolen token's blast radius in Spring Security 7 |
| 3 | [least-privilege-with-kubernetes-rbac](least-privilege-with-kubernetes-rbac) | Kubernetes Role and ClusterRole manifests evaluated and linted, checked against a real kind cluster |
| 4 | [patching-and-dependency-hygiene](patching-and-dependency-hygiene) | An SBOM, vulnerable and transitive components, CVSS 3.1 scores, patch windows and checksum pinning |
| 5 | [patching-and-dependency-hygiene-with-trivy](patching-and-dependency-hygiene-with-trivy) | Trivy scanning the SBOM the Java code writes, and Gradle dependency verification |
| 6 | [backups-and-recovery](backups-and-recovery) | Full and incremental backups, restore tests, 3-2-1, ransomware, retention and sealed archives |
| 7 | [backups-and-recovery-with-rustfs](backups-and-recovery-with-rustfs) | Object versioning, object lock and a write-only backup identity on a local S3-compatible store |
| 8 | [least-privilege-with-terraform](least-privilege-with-terraform) | IAM policies as Terraform applied with OpenTofu to LocalStack, evaluated in Java with deny, boundary, blast radius and a linter |

A project with `-with-` in its name is the same lesson with a framework, a library or a local tool, and is its own self-contained project.

## How the module is judged

- **Break it first.** Each project shows the naive version failing before any control appears.
- **Every claim is a number.** Each failure and each fix prints an exact result that a test checks, against a golden output file.
- **The bill is part of the lesson.** The last act of every project shows what the control costs, or what it still leaves open.
- **A threat model for every project,** with a diagram, in `docs/threat-model.md`.

See the [learning path](../learning-path.md) for the order of all projects.
