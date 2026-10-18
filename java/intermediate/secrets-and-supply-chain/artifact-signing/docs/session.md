# Session Guide — Artifact Signing: Signed Releases, Key Rotation and Transparency Logs

## Learning Objectives

By the end of the session you can:

- explain why a checksum next to a download does not prove integrity
- sign and verify a release statement that covers the exact bytes
- rotate and revoke a signing key without breaking deployments
- explain why the signer's own date cannot settle revocation
- describe what a transparency log and its inclusion and consistency proofs prove

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: A deployment that runs whatever arrives | 7 min |
| 0:17 | Act 2: A signed release, checked before it runs | 7 min |
| 0:24 | Act 3: Signing keys: per channel, rotated, revoked | 7 min |
| 0:31 | Act 4: A public log that every signature must be in | 7 min |
| 0:38 | Act 5: The bill | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Start with the sealed crates. Run act one and ask who decided what runs in production. In act two, ask why the statement carries the digest and not the jar. In act three, ask
what the backdated signature tells about trusting dates. In act four, ask what the monitor would need to know to spot the bad entry. In act five, ask which deployment paths in
a real store would skip the gate.

## Exercises

1. Add a second monitor that keeps the last head it saw and checks consistency with every new head.
2. Make the gate refuse a release whose version is lower than the one already deployed, to stop a rollback to an old signed jar.
3. Sign with two keys and require both, as a two-person rule for releases.
4. Add a log key rotation: the verifier trusts two log keys for a while.
