# TLS in practice

The beginner track explained the handshake. This module builds and breaks the pieces around it: a small certificate authority that issues, renews and revokes
real X.509 certificates, the misconfigurations that a checker should find, what a key log gives away, and what pinning costs.

## The projects

| # | Project | What it shows | Maps to |
| --- | --- | --- | --- |
| 1 | [local-certificate-authority](local-certificate-authority) | A certificate is a signed claim about a name, and revocation is only as good as the list the client has | A04, CWE-295, CWE-299 |
| 2 | [tls-misconfigurations](tls-misconfigurations) | A server's TLS is a set of choices, and a checker can read all of them | A02, A04, CWE-326, CWE-327, CWE-295 |
| 3 | [inspecting-tls-traffic](inspecting-tls-traffic) | A key log opens exactly the connections it lists, and a leaked server key opens nothing that was ephemeral | A04, CWE-312, CWE-532 |
| 4 | [certificate-pinning](certificate-pinning) | A pin trusts one key instead of every authority, and the price is paid on the day the key changes | A04, CWE-295, CWE-296 |

## How the module is judged

- **Real formats.** Certificates and revocation lists are real DER, read back by the JDK's own PKIX code.
- **Every claim is a number** that a test checks against a golden output file.
- **A threat model for every project,** with a diagram, in `docs/threat-model.md`.
