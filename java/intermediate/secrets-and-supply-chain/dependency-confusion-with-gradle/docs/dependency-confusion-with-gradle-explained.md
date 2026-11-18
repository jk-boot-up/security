# Dependency Confusion with Gradle, Explained

## The idea in one sentence

A build should ask only the repository that owns a name, and should check the bytes it gets against a list it reviewed.

## The 5 acts

### 1. A build that works

The input: two repositories, asked in this order: private (the store's own; only the store publishes) and public (anyone can publish). The dependencies: org.acme:json-lib:2.0.1, com.shop:pricing:[1.4,) (1.4 or higher) and com.shop:audit:3.1.0.
The result, from a real Gradle build: com.shop:audit:3.1.0 and com.shop:pricing:1.4.2 from private, org.acme:json-lib:2.0.1 from public. Each jar carries an origin.txt saying who published it, so the output shows where a jar really came from.

### 2. Somebody registers the store's name in the public repository

The input: the attacker, who knows only the name, publishes com.shop:pricing:99.0.0 to the public repository.
How Gradle picks a version for com.shop:pricing:[1.4,): it reads maven-metadata.xml in every repository. private lists 1.4.2, public lists 99.0.0. Both are at or above 1.4. Compared number by number, 99 > 1, so the highest is 99.0.0.
The result: com.shop:pricing:99.0.0 from public (the attacker's). Then, with public declared first and the attacker's copy of com.shop:audit:3.1.0, the exact pin does not help: the first repository that has it answers, and audit 3.1.0 also comes from the attacker.

### 3. The repository says which names it may answer for

The input: a content { } block per repository in settings.gradle. private: includeGroupAndSubgroups 'com.shop'. public: excludeGroupAndSubgroups 'com.shop'.
The result, with the attacker's packages still in public: com.shop:audit:3.1.0 and com.shop:pricing:1.4.2 from private, json-lib from public. The public repository's log has 0 requests under com/shop: the internal name never left the building.
An internal library not published yet, com.shop:newlib:1.0: with the filters the build is refused ("could not find com.shop:newlib:1.0") and public received 0 requests for com/shop. Without the filters, the same build asks public for /com/shop/newlib/1.0/newlib-1.0.pom: the name leaks, and so does the chance to answer it.

### 4. Checksums that the build checks

The input: a trusted build runs once with --write-verification-metadata sha256, and Gradle writes gradle/verification-metadata.xml. For pricing-1.4.2.jar it lists a SHA-256; the demo computes the SHA-256 of private's jar itself, and it is the same. The same build, checked against the metadata, resolves as before.
How the check is made, for the com.shop:pricing jar: the metadata lists 1.4.2 with its checksum. The attacker's 99.0.0 has another checksum and is not listed at all, so the build that the range would hand to the attacker is refused: "not listed in gradle/verification-metadata.xml".
A replaced 1.4.2 in private (same version number, other bytes) has a different SHA-256, so it is refused too: "checksum differs from gradle/verification-metadata.xml".

### 5. The bill

The input: the attacker publishes com.shop.tools:signer:9.0, and the store asks for com.shop.tools:signer:[1.0,).
How a filter matches: includeGroup 'com.shop' covers com.shop only, so it does not cover com.shop.tools; includeGroupAndSubgroups 'com.shop' does. The result with includeGroup and excludeGroup: signer 9.0 from public (the attacker's). With the group and its subgroups: signer 1.0 from private.
Checksums written with --write-verification-metadata sha256 after the attacker published, and without filters, record the attacker's pricing 99.0.0: the metadata trusts whatever was resolved. Still to do: the plugin repositories (pluginManagement) need the same filters, and a real repository uses https, not http on loopback.

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
