# Advanced track plan: Specialized Topics (Java)

The advanced track assumes the beginner and intermediate tracks. It moves from one application to systems: platforms, pipelines,
service identity, detection at scale, cryptographic change, and how security is run and governed. It follows the references used by
practitioners in these areas rather than a fixed list:

| Reference | Where it shapes this track |
| --- | --- |
| NIST CSF 2.0, NIST SP 800-53 (selected controls), ISO/IEC 27001 Annex A (concepts) | Modules 9 and 10 |
| CIS Benchmarks (Docker, Kubernetes), NSA/CISA Kubernetes Hardening Guide | Modules 2 and 3 |
| NIST SP 800-207 (zero trust), SPIFFE | Module 5 |
| SLSA, OpenSSF Scorecard, NIST SSDF | Module 4 |
| NIST post-quantum standards: FIPS 203 (ML-KEM), FIPS 204 (ML-DSA) | Module 6 |
| MITRE ATT&CK, Sigma | Module 7 |
| OWASP WSTG, PTES | Module 8 |

The conventions are the same as in the other tracks:
- one self-contained Java 25 + Gradle project per concept, under `java/advanced/<module>/<project>/`, with `level = "advanced"`;
- plain and `-with-<tool>` versions, five acts, a threat model with a diagram, golden output, animation and video;
- local labs only, no cloud account;
- latest stable versions, no deprecated API;
- nothing uploaded before review.

## Modules and projects

### 1. cloud-security-without-a-cloud
| # | Project | What it teaches | Tools |
| ---: | --- | --- | --- |
| 1 | `cloud-iam-models` | Identity, resource and session policies, role assumption and confused-deputy attacks, evaluated in Java | Java only; an emulator comes with `cloud-misconfigurations` |
| 2 | `network-isolation` | Segmentation, security groups as code, and what a flat network lets an attacker reach | Docker networks, kind with network policies |
| 3 | `cloud-misconfigurations` | Public buckets, wildcard policies, metadata endpoints and unencrypted storage, found before deploy | Moto (LocalStack now needs an account token), Checkov, Trivy |

### 2. infrastructure-as-code-security
| # | Project | What it teaches | Tools |
| ---: | --- | --- | --- |
| 4 | `iac-scanning` | Writing a rule, then running scanners on OpenTofu and Kubernetes manifests, and handling false positives and exceptions | Checkov, Trivy, KICS |
| 5 | `policy-as-code` | Deny-by-default admission and deployment rules written as policy, with tests | OPA and Rego, Conftest |

### 3. kubernetes-and-container-security
| # | Project | What it teaches | Tools |
| ---: | --- | --- | --- |
| 6 | `kubernetes-admission-control` | Blocking privileged pods, unsigned images and missing limits at admission | kind, Kyverno |
| 7 | `kubernetes-network-policies` | Default-deny traffic between namespaces and pods, and testing it | kind, Calico or Cilium |
| 8 | `runtime-security` | Detecting a shell in a container, unexpected writes and outbound connections | kind, Falco |
| 9 | `kubernetes-secrets` | Why base64 is not encryption; encryption at rest, external secrets and short-lived tokens | kind, OpenBao |

### 4. devsecops-pipelines
| # | Project | What it teaches | Tools |
| ---: | --- | --- | --- |
| 10 | `sast` | Static analysis: how taint tracking finds injection, writing a custom rule, triage | Semgrep, CodeQL CLI |
| 11 | `dast` | Scanning the running intermediate store, authenticated scans, and reading a report | OWASP ZAP |
| 12 | `secret-scanning` | Finding secrets in history and blocking them before push | gitleaks, pre-commit hooks |
| 13 | `secure-pipeline` | A local pipeline with signed, reproducible builds, provenance and gates that fail closed | Gradle, cosign, SLSA provenance, a local Gitea and Woodpecker CI |

### 5. service-identity-and-zero-trust
| # | Project | What it teaches | Tools |
| ---: | --- | --- | --- |
| 14 | `mutual-tls-between-services` | Client certificates for service-to-service calls, authorization by identity, and rotation | Spring Boot, step-ca |
| 15 | `service-mesh-mtls` | Mesh-issued identities and automatic mTLS, and what the mesh does not protect | kind, Linkerd |
| 16 | `workload-identity` | SPIFFE IDs and short-lived SVIDs instead of shared secrets | SPIRE |
| 17 | `zero-trust-access` | Every request authenticated, authorized and logged, close to the resource; an identity-aware proxy | Keycloak, OPA, Envoy or oauth2-proxy |

### 6. applied-cryptography-and-pki
| # | Project | What it teaches | Tools |
| ---: | --- | --- | --- |
| 18 | `certificate-lifecycle` | Issuance, OCSP and CRLs, stapling, short-lived certificates and automated rotation, and the outages that missed renewals cause | step-ca, OpenSSL |
| 19 | `tls-attacks-and-downgrades` | Downgrades, weak key exchange, and why TLS 1.3 removed what it removed, shown on deliberately weak local servers | OpenSSL, testssl.sh |
| 20 | `post-quantum-cryptography` | ML-KEM and ML-DSA in the JDK, hybrid key exchange, and planning a migration | JDK 25, Bouncy Castle |
| 21 | `crypto-agility` | Inventorying algorithms, versioned formats and rotating an algorithm without losing data | JDK 25, Tink |
| 22 | `hardware-backed-keys` | Keys that never leave a token; signing with PKCS#11 | SoftHSM, the JDK PKCS#11 provider |

### 7. detection-engineering
| # | Project | What it teaches | Tools |
| ---: | --- | --- | --- |
| 23 | `detection-as-code` | Sigma rules mapped to ATT&CK, tested against recorded logs, with false-positive budgets | Sigma, pySigma, OpenSearch |
| 24 | `siem-basics` | Collecting, normalizing and correlating events from the store, the database and the hosts | Wazuh or OpenSearch Security Analytics |
| 25 | `threat-hunting` | Hypothesis-driven hunts over a week of simulated activity | OpenSearch, Jupyter |

### 8. offensive-security-methodology
| # | Project | What it teaches | Tools |
| ---: | --- | --- | --- |
| 26 | `vulnerability-research` | From a crash found by fuzzing to a root cause, a patch and an advisory, with coordinated disclosure | Jazzer, a debugger |
| 27 | `reverse-engineering-fundamentals` | Reading Java bytecode and a decompiled jar, finding a hard-coded key, and obfuscation's limits | javap, CFR, Ghidra |
| 28 | `malware-analysis-fundamentals` | Static triage of a harmless, purpose-built sample: strings, hashes, YARA rules and sandbox safety | YARA, a disposable container |

### 9. resilience-and-recovery
| # | Project | What it teaches | Tools |
| ---: | --- | --- | --- |
| 29 | `ransomware-resilience` | Immutable backups, isolated recovery, and a timed restore exercise | RustFS object lock, OpenTofu |
| 30 | `security-chaos-engineering` | Injecting failures into security controls and checking that they fail closed and alert | Toxiproxy, the intermediate store |

### 10. governance-risk-and-compliance
| # | Project | What it teaches | Tools |
| ---: | --- | --- | --- |
| 31 | `risk-management` | A risk register with quantified risk (FAIR-style ranges), treatment decisions and residual risk | Java, Monte Carlo simulation |
| 32 | `controls-and-evidence` | Mapping controls to NIST CSF 2.0 and ISO 27001, and collecting evidence automatically from tests | Java, the earlier projects' tests |
| 33 | `security-architecture-review` | Reviewing a design: trust boundaries, data flows, and decision records | Structurizr (C4), ADRs |
| 34 | `privacy-engineering` | Data minimization, pseudonymization, retention and subject access in a bank's data model | Java, PostgreSQL |

## Order of work

After the intermediate track. Modules 1 to 4 first, because they set up the platforms and pipelines that later modules use, then 5 to 10.
