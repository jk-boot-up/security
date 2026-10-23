# Offensive security methodology

Testing, research, reverse engineering and malware triage

## The projects

| # | Project | What it shows |
| --- | --- | --- |
| 1 | [vulnerability-research](vulnerability-research) | Fuzz a bank's statement reader with Jazzer, take the crashes to their root causes, fix them, prove the fix, and write the advisory with a coordinated disclosure timeline |
| 2 | [reverse-engineering-fundamentals](reverse-engineering-fundamentals) | Read a bank client's class files with the JDK's class file API, javap and the CFR decompiler, find a hard-coded key, patch a licence check, and see what obfuscation does and does not stop |
| 3 | [malware-analysis-fundamentals](malware-analysis-fundamentals) | Triage harmless, purpose-built samples with hashes, strings, entropy and YARA rules, and run one in a disposable container to see what a sandbox stops |

The module plan is in [`../../../docs/advanced-plan.md`](../../../docs/advanced-plan.md). Run a project as in the other tracks: `./gradlew test`, then `./gradlew run`.
