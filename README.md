# Security Learning Path

A structured, hands-on collection of notes, guides, and exercises for learning **security concepts from beginner to advanced**. The goal is to build understanding step by step: first the fundamentals, then how real systems are attacked and defended, and finally the deeper, specialized topics.

> **Status:** Draft. Content is being added incrementally.

## Who this is for

- Beginners who are new to security and want a clear starting point
- Developers who want to write more secure code
- Engineers moving into security, DevSecOps, or cloud security
- Anyone preparing for security certifications or interviews

## Scope: no cloud account required

Every topic here can be learned **without an account on any commercial cloud provider**, including free tiers. Tools may be free or paid, but **all labs must run locally** on your own machine using VMs, containers, and open-source or self-hosted platforms. Hosted lab services and online practice platforms are not relied on.

Topics that depend on a real AWS, Azure, or GCP account (provider-specific IAM behavior, billing, managed-service quirks) are intentionally **out of scope**.

## Learning path

### 1. Beginner: Foundations
- What security is: the CIA triad (confidentiality, integrity, availability)
- Threats, vulnerabilities, risks, and attack surfaces
- Authentication vs. authorization
- Passwords, hashing, and multi-factor authentication
- Basic cryptography: symmetric, asymmetric, hashing, digital signatures
- Networking basics for security: TCP/IP, DNS, HTTP/HTTPS
- Transport security basics: the TLS handshake step by step, certificates and the chain of trust, what HTTPS protects and what it doesn't
- Safe habits: patching, least privilege, backups

### 2. Intermediate: Applied Security
- Web application security: OWASP Top 10 (XSS, SQL injection, CSRF, SSRF, and more)
- API security and session management
- Identity and access management: OAuth 2.0, OpenID Connect, JWT, SAML
- TLS in practice: running a local CA (OpenSSL, step-ca), configuring TLS on nginx/Caddy, HSTS, certificate pinning, inspecting traffic with Wireshark, common misconfigurations
- Secure coding practices and input validation
- Secrets management and dependency/supply chain hygiene
- Linux and OS hardening
- Logging, monitoring, and basic incident response
- Threat modeling (STRIDE, attack trees)

### 3. Advanced: Specialized Topics
- Cloud security concepts on open-source, self-hosted platforms (LocalStack, OpenStack, MinIO, Keycloak): IAM models, network isolation, misconfiguration risks
- Infrastructure-as-code security: writing and scanning Terraform/Kubernetes manifests (Checkov, tfsec, Trivy) without deploying to a provider
- Container and Kubernetes security (Docker, kind, minikube, k3d)
- DevSecOps: SAST, DAST, SCA, and CI/CD pipeline security
- Applied cryptography and PKI
- Mutual TLS (mTLS) between services, with client certificates
- Certificate lifecycle: issuance, revocation (CRL, OCSP), and rotation
- Service mesh mTLS (Istio, Linkerd) on a local kind cluster, and SPIFFE/SPIRE workload identity
- TLS attacks and downgrade issues
- Zero trust architecture
- Detection engineering and SIEM
- Vulnerability research and penetration testing methodology
- Malware analysis and reverse engineering fundamentals
- Security architecture and governance, risk, and compliance (GRC)

## Repository layout

The structure will grow as content is added. Planned layout:

```
security/
├── java/                         # Java 25 + Gradle (first language)
│   ├── beginner/                 # Foundations and core concepts
│   │   └── <module>/<project>/   # one self-contained project per concept
│   ├── intermediate/             # Applied and application security
│   └── advanced/                 # Specialized and in-depth topics
├── go/                           # added later, same shape
├── cpp/                          # added later, same shape
├── docs/                         # plans and specs
├── tools/                        # shared build, video and publishing tools
├── cheatsheets/                  # Quick references
└── resources/                    # Books, courses, tools, and links
```

Each concept is its own project, and a version that uses a framework or tool is a separate
sibling project (for example `password-hashing` and `password-hashing-with-spring-security`).
See [`docs/beginner-plan.md`](docs/beginner-plan.md).

## How to use this repo

1. Start at `java/beginner/` and work through the modules in order.
2. Run each project's demo and tests before moving on.
3. Use `cheatsheets/` for quick review.
4. Revisit earlier sections as you go deeper. Many advanced topics build on the basics.

## Ethics and responsible use

Everything here is for **education and defensive purposes**. Only test systems you own or have explicit written permission to test. Practice only in local lab environments, such as VMs and containers running intentionally vulnerable apps.

## Contributing

This is a personal learning repo, but corrections and suggestions are welcome.
