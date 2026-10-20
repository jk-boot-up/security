# TLS in practice

The beginner track explained the handshake. This module builds and breaks the pieces around it: a small certificate authority that issues, renews and revokes
real X.509 certificates, the misconfigurations that a checker should find, what a key log gives away, and what pinning costs.

## The projects

| # | Project | What it shows | Maps to |
| --- | --- | --- | --- |
| 1 | [local-certificate-authority](local-certificate-authority) | A certificate is a signed claim about a name, and revocation is only as good as the list the client has | A04, CWE-295, CWE-299 |

## How the module is judged

- **Real formats.** Certificates and revocation lists are real DER, read back by the JDK's own PKIX code.
- **Every claim is a number** that a test checks against a golden output file.
- **A threat model for every project,** with a diagram, in `docs/threat-model.md`.
