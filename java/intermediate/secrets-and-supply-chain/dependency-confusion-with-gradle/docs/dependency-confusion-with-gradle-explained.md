# Dependency Confusion with Gradle, Explained

## The idea in one sentence

A build should ask only the repository that owns a name, and should check the bytes it gets against a list it reviewed.

## The 5 acts

### 1. A build that works

The consumer build declares the private repository first and the public one second. pricing (range [1.4,)), audit 3.1.0 and json-lib 2.0.1 are resolved: the two internal libraries from the private repository, json-lib from the public one.

### 2. Somebody registers the company's name in the public repository

The attacker publishes pricing 99.0.0 in the public repository. The range [1.4,) takes the highest version it can find in any repository: the attacker's. An exact version is safer, but with the public repository declared first, the attacker's copy of audit 3.1.0 is the first one found, and it is used.

### 3. The repository says which names it may answer for

The private repository includes the group com.shop and its subgroups, and the public repository excludes them. The same build resolves pricing 1.4.2 and audit 3.1.0 from the private repository, and the public repository received zero requests for com.shop. A library that is not published yet is refused, and still the public repository was not asked. Without the filter, the build asks the public repository for the internal name: the name leaks, and so does the chance to answer it.

### 4. Checksums that the build checks

gradle --write-verification-metadata sha256 records the checksums in gradle/verification-metadata.xml. The same build with the metadata passes. When the attacker's 99.0.0 starts to win the range, the build is refused: it is not listed. When the private repository serves the same version number with other bytes, the build is refused: the checksum differs.

### 5. The bill

A filter for the exact group com.shop does not cover com.shop.tools: the attacker's signer 9.0 is taken from the public repository. With the group and its subgroups, the private signer 1.0 is used. Checksums written while the attacker's package was already being resolved are the attacker's checksums, and the build then accepts it. The repositories for plugins and the build script are a separate list. The repositories here are plain http on loopback; a real repository is https.

## The verdict

Declare which repository may answer for each group, use exact versions or locks, keep verification metadata under review, give plugins the same rules, and publish your internal groups in the public registry too, as placeholders, where that is possible.

## How to recognise this in code you did not write

- `content { includeGroup ... }` or `exclusiveContent`.
- `gradle/verification-metadata.xml`.
- `repositoriesMode = FAIL_ON_PROJECT_REPOS`.
- A version such as `1.+` or `[1.4,)`.

## Where you have already met this

- `mavenCentral()` and a company repository in the same `repositories` block.
- `content { includeGroup ... }` or `exclusiveContent` in a build file.
- A `verification-metadata.xml` in the `gradle` folder.
- A version such as `1.+` or `[1.4,)` in a dependency.
